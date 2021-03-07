package io.github.waikato_ufdl.ui.manage;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;

/***
 * Class which encapsulates the dataset information.
 */

public class ImageDataset {
    private boolean isSelected;
    private final int primaryKey;
    private String name;
    private final String description;
    private final int project;
    private final int license;
    private final boolean isPublic;
    private final String tags;
    private int syncStatus;

    /***
     * Constructor for the image dataset
     * @param primaryKey the primary key of the dataset
     * @param name the name of dataset
     * @param description the dataset description
     * @param project an integer representing the project
     * @param license an integer representing the license
     * @param isPublic a boolean value indicating whether the dataset is public
     * @param tags the dataset tags
     * @param syncStatus the sync status of the dataset
     */
    public ImageDataset(int primaryKey, String name, String description, int project, int license, boolean isPublic, String tags, int syncStatus) {
        this.syncStatus = 0;
        this.primaryKey = primaryKey;
        this.name = name;
        this.description = description;
        this.project = project;
        this.license = license;
        this.isPublic = isPublic;
        this.tags = tags;
        this.isSelected = false;
        this.syncStatus = syncStatus;
    }

    /***
     * Get the selection state of the dataset
     * @return True if the dataset is selected. False if the dataset is not selected.
     */
    public boolean isSelected() {
        return isSelected;
    }

    /***
     * Set the selection state of the dataset
     * @param isSelected true if dataset should be selected. False if the dataset should not be selected.
     */
    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    /***
     * Get the primary key of the dataset
     * @return primary key of the dataset
     */
    public int getPK() {
        return primaryKey;
    }

    /***
     * Get the name of the dataset
     * @return the name of the dataset
     */
    public String getName() {
        return name;
    }

    /***
     * Set the name of the dataset
     * @param name the new name for the dataset
     */
    public void setName(String name) {
        this.name = name;
    }

    /***
     * Get the description text of the dataset
     * @return the description text of the dataset
     */
    public String getDescription() {
        return description;
    }

    /***
     * Get the project of the dataset
     * @return the integer value representing the project of the dataset
     */
    public int getProject() {
        return project;
    }

    /***
     * Get the license of the dataset
     * @return the integer value representing the license of the dataset
     */
    public int getLicense() {
        return license;
    }

    /***
     * Get the boolean value indicating whether the dataset is public
     * @return true if dataset is public. Else, false.
     */
    public boolean isPublic() {
        return isPublic;
    }

    /***
     * Get the dataset tags
     * @return the dataset tags
     */
    public String getTags() {
        return tags;
    }

    /***
     * Get the sync status of the dataset
     * @return integer value representing the sync status. Where, 0 = Synced, 1 = Create, 2 = Update or 3 = Delete.
     */
    public int getSyncStatus() {
        return syncStatus;
    }

    /***
     * Compares a dataset object to another object.
     * @param o the object to compare
     * @return true if the objects are equal. False, if the objects are not equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageDataset dataset = (ImageDataset) o;
        return isSelected == dataset.isSelected &&
                primaryKey == dataset.primaryKey &&
                project == dataset.project &&
                license == dataset.license &&
                isPublic == dataset.isPublic &&
                syncStatus == dataset.syncStatus &&
                name.equals(dataset.name) &&
                Objects.equals(description, dataset.description) &&
                Objects.equals(tags, dataset.tags);
    }

    /***
     * Returns a hash code value for the object.
     * @return a hash code value for the object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(isSelected, primaryKey, name, description, project, license, isPublic, tags, syncStatus);
    }

    /***
     * The Diff-Util item callback for calculating the diff between two non-null items in a list.
     */
    public static DiffUtil.ItemCallback<ImageDataset> datasetItemCallback = new DiffUtil.ItemCallback<ImageDataset>() {

        /***
         * Called to check whether two objects represent the same item.
         * @param oldItem the item in the old list
         * @param newItem the item in the new list
         * @return True if the two items represent the same object or false if they are different.
         */
        @Override
        public boolean areItemsTheSame(@NonNull ImageDataset oldItem, @NonNull ImageDataset newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getPK() == newItem.getPK();
        }

        /***
         * Called to check whether two items have the same data.
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True if the contents of the items are the same or false if they are different.
         */
        @Override
        public boolean areContentsTheSame(@NonNull ImageDataset oldItem, @NonNull ImageDataset newItem) {
            return oldItem.equals(newItem);
        }
    };
}

