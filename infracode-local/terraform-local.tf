terraform {
  backend "remote" {
    organization = "FairfieldBytes"

    workspaces {
      name = "daily-app-local"
    }
  }
}

provider "aws" {
  profile = "default"
  region  = "us-east-1"
}

resource "aws_s3_bucket" "bucket_store" {
  bucket = "daily-app-local-bucket-store"
  acl    = "private"

  tags = {
    Name        = "Daily App Local Bucket Store"
    Environment = "Local"
  }
}

resource "aws_s3_bucket_public_access_block" "s3_public_block" {
  bucket = "${aws_s3_bucket.bucket_store.id}"

  block_public_acls = true
  block_public_policy = true
  ignore_public_acls = true
  restrict_public_buckets = true
}
