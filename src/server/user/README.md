### AWS S3 설정
```java
@Configuration
public class AWSConfig {
    @Value("${cloud.aws.credentials.accessKey}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secretKey}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean
    public AmazonS3Client amazonS3Client() {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey,secretKey);
        return (AmazonS3Client) AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                .build();
    }
}
```
---
### AWS S3 이미지 uri 업로드/다운로드
```java
public ResponseProfile getProfile(String target) {
    UserEntity userEntity = userRepository.findByUserIdOrEmail(target,target);

    if (userEntity == null) {
        throw new UserNotFoundException(String.format("[%s] is Not Found", target));
    } else {
        ResponseProfile responseProfile = new ResponseProfile(
                userEntity.getUserId(), userEntity.getEmail(),userEntity.getProfile());
        return responseProfile;
    }
}

public ResponseProfile putProfile(RequestProfile profile) {
    UserEntity userEntity = userRepository.findByUserIdOrEmail(profile.getTarget(),profile.getTarget());

    if (userEntity == null) {
        throw new UserNotFoundException(String.format("[%s] is Not Found",profile.getTarget()));
    }
    MultipartFile multipartFile = profile.getImg();
    String s3FileName = UUID.randomUUID() + "-" + multipartFile.getOriginalFilename();

    ObjectMetadata objMeta = new ObjectMetadata();
    try {
        objMeta.setContentLength(multipartFile.getInputStream().available());

        amazonS3Client.putObject(S3Bucket, s3FileName, multipartFile.getInputStream(), objMeta);
        String uri = amazonS3Client.getUrl(S3Bucket, s3FileName).toString();

        userEntity.setProfile(Map.of(
                "nickname", profile.getNickname(),
                "img", uri
        ));
        UserEntity savedUser = userRepository.save(userEntity);
        ResponseProfile responseProfile = new ResponseProfile(
                savedUser.getUserId(), savedUser.getEmail(), savedUser.getProfile()
        );
        return responseProfile;
    } catch (IOException e) {
        e.printStackTrace();
        throw new FileStreamException("file streaming is failed");
    }
}
```
---
### 친구 상태 
```java
public enum FriendshipCode {
    NONE(0),
    REQUESTED(1),
    ACCCEPTED(2),
    REJECTED(3),
    BLOCKED(9),

    ;

    private final int status;

    FriendshipCode(int status) {
        this.status = status;
    }
    public int value() {
        return this.status;
    }
}
```
