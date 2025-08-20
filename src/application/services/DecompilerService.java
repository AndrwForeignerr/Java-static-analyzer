package application.services;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DecompilerService {
    
    private static final String CFR_JAR_PATH = "lib/cfr-0.152.jar";
    private final Path tempDirectory;
    
    public DecompilerService() {
        try {
            tempDirectory = Files.createTempDirectory("java_analyzer_temp");
            tempDirectory.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary directory", e);
        }
    }
    
    public String decompile(File classFile) throws Exception {
        validateClassFile(classFile);
        
        String outputPath = tempDirectory.toString();
        String decompiledCode = executeCFRDecompilation(classFile, outputPath);
        
        if (decompiledCode == null || decompiledCode.trim().isEmpty()) {
            throw new RuntimeException("Decompilation failed - no output generated");
        }
        
        return decompiledCode;
    }
    
    private void validateClassFile(File classFile) throws Exception {
        if (!classFile.exists()) {
            throw new FileNotFoundException("Class file not found: " + classFile.getPath());
        }
        
        if (!classFile.getName().endsWith(".class")) {
            throw new IllegalArgumentException("File must have .class extension");
        }
        
        if (!isValidClassFile(classFile)) {
            throw new IllegalArgumentException("Invalid class file format");
        }
    }
    
    private boolean isValidClassFile(File classFile) {
        try (FileInputStream fis = new FileInputStream(classFile)) {
            byte[] header = new byte[4];
            int bytesRead = fis.read(header);
            
            if (bytesRead != 4) return false;
            
            return header[0] == (byte) 0xCA && 
                   header[1] == (byte) 0xFE && 
                   header[2] == (byte) 0xBA && 
                   header[3] == (byte) 0xBE;
        } catch (IOException e) {
            return false;
        }
    }
    
    private String executeCFRDecompilation(File classFile, String outputPath) throws Exception {
        List<String> command = buildCFRCommand(classFile, outputPath);
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        String output = readProcessOutput(process);
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("CFR decompilation failed with exit code: " + exitCode + "\nOutput: " + output);
        }
        
        return findAndReadDecompiledFile(classFile, outputPath);
    }
    
    private List<String> buildCFRCommand(File classFile, String outputPath) {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(CFR_JAR_PATH);
        command.add(classFile.getAbsolutePath());
        command.add("--outputdir");
        command.add(outputPath);
        command.add("--comments");
        command.add("false");
        command.add("--showversion");
        command.add("false");
        
        return command;
    }
    
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        return output.toString();
    }
    
    private String findAndReadDecompiledFile(File originalClassFile, String outputPath) throws IOException {
        String className = originalClassFile.getName().replace(".class", "");
        Path decompiledFile = Paths.get(outputPath, className + ".java");
        
        if (!Files.exists(decompiledFile)) {
            decompiledFile = findDecompiledFileRecursively(Paths.get(outputPath), className);
        }
        
        if (decompiledFile == null || !Files.exists(decompiledFile)) {
            throw new FileNotFoundException("Decompiled file not found for class: " + className);
        }
        
        return Files.readString(decompiledFile);
    }
    
    private Path findDecompiledFileRecursively(Path directory, String className) throws IOException {
        return Files.walk(directory)
                   .filter(path -> path.getFileName().toString().equals(className + ".java"))
                   .findFirst()
                   .orElse(null);
    }
    
    public boolean isCFRAvailable() {
        File cfrJar = new File(CFR_JAR_PATH);
        return cfrJar.exists() && cfrJar.canRead();
    }
    
    public void cleanup() {
        try {
            if (Files.exists(tempDirectory)) {
                Files.walk(tempDirectory)
                     .sorted((a, b) -> b.compareTo(a))
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                         } catch (IOException e) {
                             System.err.println("Failed to delete: " + path);
                         }
                     });
            }
        } catch (IOException e) {
            System.err.println("Failed to cleanup temporary directory: " + e.getMessage());
        }
    }
}