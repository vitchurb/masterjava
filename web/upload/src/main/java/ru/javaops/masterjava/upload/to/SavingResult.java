package ru.javaops.masterjava.upload.to;

import java.util.Collection;
import java.util.List;

/**
 * Created by vit on 13.11.2017.
 */
public class SavingResult {
    private final List<SaveUserResult> usersWithError;
    private final Collection<SaveChunkError> chunksWithError;

    public SavingResult(List<SaveUserResult> usersWithError, Collection<SaveChunkError> chunksWithError) {
        this.usersWithError = usersWithError;
        this.chunksWithError = chunksWithError;
    }

    public List<SaveUserResult> getUsersWithError() {
        return usersWithError;
    }

    public Collection<SaveChunkError> getChunksWithError() {
        return chunksWithError;
    }
}
