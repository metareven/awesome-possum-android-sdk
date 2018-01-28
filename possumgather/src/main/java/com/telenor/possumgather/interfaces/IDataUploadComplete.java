package com.telenor.possumgather.interfaces;

public interface IDataUploadComplete {
    void dataUploadFailed(Exception e);
    void dataUploadSuccess();
}