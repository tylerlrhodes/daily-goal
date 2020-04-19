terraform {
  backend "remote" {
    organization = "FairfieldBytes"

    workspaces {
      name = "daily-app"
    }
  }
}
    
provider "aws" {
  profile = "default"
  region  = "us-east-1"
}

locals {
  domain_name = "fairfieldbytes.com"
  record_name = "daily-dev"
  pub_key_path = "artifacts/daily_ec2_dev_key.pub"
}

resource "aws_key_pair" "generated_key" {
  key_name   = "dev_key"
  public_key = file(local.pub_key_path)
}

resource "aws_vpc" "vpc" {
  cidr_block = "10.0.0.0/24"
}

resource "aws_internet_gateway" "gw" {
  vpc_id = aws_vpc.vpc.id
}

resource "aws_subnet" "subnet" {
  vpc_id = aws_vpc.vpc.id
  cidr_block = aws_vpc.vpc.cidr_block  
}

resource "aws_route_table" "r" {
  vpc_id = aws_vpc.vpc.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.gw.id
  }
}

resource "aws_route_table_association" "a" {
  subnet_id = aws_subnet.subnet.id
  route_table_id = aws_route_table.r.id
}

resource "aws_security_group" "sec_grp" {
  name = "def security"
  vpc_id = aws_vpc.vpc.id
  ingress {
    from_port = 22
    to_port = 22
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port = 80
    to_port = 80
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port = 443
    to_port = 443
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

data "aws_route53_zone" "ffb_zone" {
  name = local.domain_name
}

resource "aws_route53_record" "daily_dev_dns" {
  zone_id = data.aws_route53_zone.ffb_zone.zone_id
  name = "${local.record_name}.${local.domain_name}"
  type = "A"
  ttl = "1"
  records = [aws_instance.web_server.public_ip]
}


resource "aws_s3_bucket" "bucket_store" {
  bucket = "daily-app-development-bucket-store"
  acl    = "private"
  tags = {
    Name        = "Daily App Development Bucket Store"
    Environment = "Development"
  }
}

resource "aws_s3_bucket_public_access_block" "s3_public_block" {
  bucket = "${aws_s3_bucket.bucket_store.id}"

  block_public_acls = true
  block_public_policy = true
  ignore_public_acls = true
  restrict_public_buckets = true
}

resource "aws_iam_role" "ec2_s3_access_role" {
  name = "s3-role"
  assume_role_policy =<<EOT
{
 "Version": "2012-10-17",
 "Statement": [
   {
     "Action": "sts:AssumeRole",
     "Principal": {
       "Service": "ec2.amazonaws.com"
     },
     "Effect": "Allow",
     "Sid": ""
   }
 ]
}
EOT
}

resource "aws_iam_policy" "s3_policy" {
  name = "ec2-access-policy"
  description = "Policy for use with IAM role and EC2"
  policy =<<EOT
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "s3:*",
            "Resource": "${aws_s3_bucket.bucket_store.arn}"
        }
    ]
}
EOT
}

resource "aws_iam_policy_attachment" "attach-policy" {
  name = "attach-policy"
  roles = [aws_iam_role.ec2_s3_access_role.name]
  policy_arn = aws_iam_policy.s3_policy.arn
}

resource "aws_iam_instance_profile" "iam_profile" {
  name = "iam_profile"
  role = aws_iam_role.ec2_s3_access_role.name
}


resource "aws_instance" "web_server" {
  ami           = "ami-085925f297f89fce1" #"ami-01b0d3176b3d530ad"
  instance_type = "t3.nano"
  subnet_id = aws_subnet.subnet.id
  associate_public_ip_address = true
  key_name      = aws_key_pair.generated_key.key_name
  vpc_security_group_ids = [aws_security_group.sec_grp.id]
  iam_instance_profile = aws_iam_instance_profile.iam_profile.name
  depends_on = [aws_internet_gateway.gw]
  tags = {
    role = "web"
    env = "dev"
  }
}



