package com.workflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RemoteShellExecutor {
    
    public String executeCommand(String host, String command) {
        try {
            // Using SSH to execute remote command (simplified)
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("ssh", host, command);
            
            Process process = processBuilder.start();
            
            // Read output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String output = reader.lines().collect(Collectors.joining("\n"));
                
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("Command execution failed with exit code: " + exitCode);
                }
                
                return output;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute remote command", e);
        }
    }
}