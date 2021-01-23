package io.github.waikato_ufdl.ui.manage;

public class ImageDataset {
    private int primaryKey;
    private String name;
    private String description;
    private int project;
    private int license;
    private boolean isPublic;
    private String tags;
    private int syncStatus;

    public ImageDataset(int primaryKey, String name, String description, int project, int license, boolean isPublic, String tags)
    {
        this.syncStatus = 0;
        this.primaryKey = primaryKey;
        this.name = name;
        this.description = description;
        this.project = project;
        this.license = license;
        this.isPublic = isPublic;
        this.tags = tags;
    }

    public ImageDataset(int primaryKey, String name, String description, int project, int license, boolean isPublic, String tags, int syncStatus)
    {
        this(primaryKey, name, description, project, license, isPublic, tags);
        this.syncStatus = syncStatus;

    }

    public int getPK() {
        return primaryKey;
    }

    public void setPK(int primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getProject() {
        return project;
    }

    public void setProject(int project) {
        this.project = project;
    }

    public int getLicense() {
        return license;
    }

    public void setLicense(int license) {
        this.license = license;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public int getSyncStatus()
    {
        return syncStatus;
    }
}

