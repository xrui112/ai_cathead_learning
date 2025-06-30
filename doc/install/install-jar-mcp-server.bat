@echo off
echo ========================================
echo Spring AI MCP Client 完整安装流程
echo ========================================

echo.
echo 步骤1: 安装JAR文件到本地Maven仓库
echo.
mvn install:install-file -Dfile="C:\Users\15505\Downloads\spring-ai-starter-mcp-client-1.0.0-20250518.221913-282.jar" -DgroupId=org.springframework.ai -DartifactId=spring-ai-starter-mcp-client -Dversion=1.0.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true

if %ERRORLEVEL% NEQ 0 (
    echo ❌ JAR文件安装失败！
    pause
    exit /b 1
)

pause