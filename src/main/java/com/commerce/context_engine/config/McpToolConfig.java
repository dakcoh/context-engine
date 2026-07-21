package com.commerce.context_engine.config;

import com.commerce.context_engine.adapter.in.mcp.CommerceKnowledgeTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

    @Bean
    ToolCallbackProvider commerceToolCallbackProvider(CommerceKnowledgeTool commerceKnowledgeTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(commerceKnowledgeTool)
                .build();
    }
}
