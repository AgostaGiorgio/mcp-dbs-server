package com.mcp.dbs;

import java.util.List;

import org.springframework.ai.tool.ToolCallback;

public interface DBTool {

    List<ToolCallback> getTools();
}