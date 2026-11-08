package com.prismite.backend.dto;

public class CreateInstanceRequest {
    private String name;
    private String version;
    private String loader;
    private String gameDirectory;
    private String javaPath;
    private String memory;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getLoader() { return loader; }
    public void setLoader(String loader) { this.loader = loader; }

    public String getGameDirectory() { return gameDirectory; }
    public void setGameDirectory(String gameDirectory) { this.gameDirectory = gameDirectory; }

    public String getJavaPath() { return javaPath; }
    public void setJavaPath(String javaPath) { this.javaPath = javaPath; }

    public String getMemory() { return memory; }
    public void setMemory(String memory) { this.memory = memory; }
}
