package com.mcp.dbs.service;

import java.util.List;

import org.springframework.ai.tool.ToolCallback;

public interface DBTool {

    List<ToolCallback> getTools();
}