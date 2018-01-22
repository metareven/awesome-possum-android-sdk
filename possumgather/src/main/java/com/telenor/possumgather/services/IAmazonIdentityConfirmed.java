package com.telenor.possumgather.services;

import com.amazonaws.services.s3.AmazonS3Client;

public interface IAmazonIdentityConfirmed {
    void foundAmazonIdentity(AmazonS3Client client);
    void failedToFindAmazonIdentity();
}