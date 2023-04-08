### 이메일 - SMTP
```java
@Service
@Slf4j
public class MailService {

    private JavaMailSender mailSender;

    private UserRepository userRepository;
    private RedisService redisService;

    private final long TIME_LIMIT = 3;

    @Autowired
    public MailService(UserRepository userRepository, RedisService redisService, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.redisService = redisService;
        this.mailSender = mailSender;
    }

    public String createCode() {
        Random random = new Random();
        StringBuffer buffer = new StringBuffer();
        String authNum = "";
        for (int i=0; i<6; i++) {
            String num = Integer.toString(random.nextInt(10));

            buffer.insert(buffer.length(), num);
        }
        authNum = buffer.toString();
        log.error("key: " + authNum);
        return authNum;
    }

    public boolean checkInfo(RequestEmailVerification info){
        UserEntity user = userRepository.findByUserId(info.getUserId());
        if (user.getEmail().equals(info.getEmail()) )
            return true;
        return false;
    }
    public boolean checkInfo(RequestVerificationCode info){
        UserEntity user = userRepository.findByUserId(info.getUserId());
        if (user.getEmail().equals(info.getEmail()) )
            return true;
        return false;
    }
    @Async
    public void send(RequestEmailVerification info, String subject) {
        if (!checkInfo(info))
            throw new EntityNotFoundException("정보가 일치하지 않습니다.");
        String authNum = createCode();
        MimeMessage message = mailHelper(info,subject, authNum);
        mailSender.send(message);
        log.error("verify-"+info.getEmail());
        redisService.setValuesWithTTL("verify-"+info.getEmail(), authNum,TIME_LIMIT);
    }
    public boolean verifyCode(RequestVerificationCode verificationCode) {
        log.error(verificationCode.toString());
        if(!checkInfo(verificationCode))
            throw new UserNotFoundException("유저 ID와 유저 Email이 일치하지 않습니다.");
        UserEntity userEntity = userRepository.findByEmail(verificationCode.getEmail());
        if (userEntity == null)
            throw new UserNotFoundException(String.format("[%s] is Not Found", userEntity.getUserId()));
        if (userEntity.getState() == 9)
            throw new WithdrawalUserException("user state is 9");
        //state :9 => 회원 탈퇴, 실제 삭제은 하지 않음, 탈퇴 후 ~ 기간 이후에 삭제하는 방식?
        userEntity.setState(1);
        userRepository.save(userEntity);

        String savedVerificationCode = redisService.getValues("verify-"+verificationCode.getEmail());
        if (savedVerificationCode == null) {
            throw new RedisNullException(verificationCode.getEmail()+" there is no verification code request or verification code is expired");
        }
        else if (savedVerificationCode.equals(verificationCode.getVerificationCode()))
            return true;
        else
            throw new IncorrectVerificationCodeException(verificationCode+" is not correct");
    }

    public MimeMessage mailHelper(RequestEmailVerification info, String subject,String authNum) {
        MimeMessage message = this.mailSender.createMimeMessage();
        try {
            message.setSubject(subject,"UTF-8");
            String htmlStr = "<h1 >" + subject  + "</h1><br>"
                    +"<h2 style=\"color:blue\"> 인증 코드: " + authNum + "</h2>";
            message.setText(htmlStr, "UTF-8", "html");
            message.addRecipients(Message.RecipientType.TO, info.getEmail());

        } catch (MessagingException e) {
            e.printStackTrace();
//            throw new RuntimeException(e);
        }
        return message;
    }


}
```
---
### 에러 핸들링
```java
@RestController
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNotFoundException(Exception e) {
        ErrorResponseDto errorResponseDto = ErrorCode.USER_NOT_FOUND.toErrorResponseDto(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }
    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateUserException(Exception e) {
        ErrorResponseDto errorResponseDto = ErrorCode.DUPLICATION_USER.toErrorResponseDto(e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponseDto);
    }
    @ExceptionHandler(WithdrawalUserException.class)
    public ResponseEntity<ErrorResponseDto> handleWithdrawalUserException(Exception e) {
        ErrorResponseDto errorResponseDto = ErrorCode.WITHDRAWAL_USER.toErrorResponseDto(e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponseDto);
    }
    @ExceptionHandler(IncorrectPasswordException.class)
    public ResponseEntity<ErrorResponseDto> handleIncorrectPasswordException(Exception e) {
        ErrorResponseDto errorResponseDto = ErrorCode.INCORRECT_PASSWORD.toErrorResponseDto(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponseDto);
    }
    @ExceptionHandler(NotAccessTokenException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessTokenException(Exception e) {
        ErrorResponseDto errorResponseDto = ErrorCode.NOT_ACCESS_TOKEN.toErrorResponseDto(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }
    @ExceptionHandler(RedisNullException.class)
    public ResponseEntity<ErrorResponseDto> handleRedisNullException(Exception e) {
        ErrorResponseDto errorResponseDto = ErrorCode.NOT_EXISTED_REFRESH_TOKEN.toErrorResponseDto(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }
    @ExceptionHandler(PasswordNotChangedException.class)
    public ResponseEntity<ErrorResponseDto> handlePasswordNotChangedException(Exception e) {
        ErrorResponseDto errorResponseDto = ErrorCode.PASSWORD_NOT_CHANGED.toErrorResponseDto(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }@ExceptionHandler(IncorrectVerificationCodeException.class)
    public ResponseEntity<ErrorResponseDto> handleVerificationCodeException(Exception e) {
        ErrorResponseDto errorResponseDto = ErrorCode.INCORRECT_VERIFICATION_CODE.toErrorResponseDto(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponseDto);
    }
    @ExceptionHandler({SignatureException.class, MalformedJwtException.class,
            UnsupportedJwtException.class,IllegalArgumentException.class, ExpiredJwtException.class
    })
    public ResponseEntity<ErrorResponseDto> handleJwtException(Exception e) {
        ErrorResponseDto errorResponseDto = ErrorCode.INCORRECT_TOKEN.toErrorResponseDto(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponseDto);
    }
}
```
---
### 에러 코드
```java
public enum ErrorCode {
    INVALID_TOKEN(401, "AUTH-001", "토큰이 유효하지 않은 경우"),
    EXPIRED_TOKEN(401, "AUTH-002", "토큰이 만료된 경우"),
    MISSING_TOKEN(401, "AUTH-003", "토큰을 전달하지 않은 경우"),
    INCORRECT_TOKEN(401, "AUTH-004", "허용된 토큰이 아닌 경우"),
    NOT_ACCESS_TOKEN(400, "AUTH-005", "액세스 토큰이 아닌 경우"),
    NOT_EXISTED_REFRESH_TOKEN(404, "AUTH-006", "저장된 리프레쉬 토큰이 없는 경우"),
    UNAUTHORIZED(401, "AUTH-007", "인증에 실패한 경우"),
    WITHDRAWAL_USER(403, "AUTH-008", "탈퇴한 회원이 요청한 경우"),
    PASSWORD_NOT_CHANGED(400, "AUTH-009", "새 비밀번호로 바꿀 수 없는 경우"),
    INCORRECT_PASSWORD(401, "AUTH-010", "비밀번호가 일치하지 않는 경우"),
    INCORRECT_VERIFICATION_CODE(401, "AUTH-011", "이메일 인증 코드가 틀린 경우"),
    USER_NOT_FOUND(404, "USER-001", "해당 유저가 존재하지 않는 경우"),
    DUPLICATION_USER(409,"USER-002","해당 유저가 이미 존재하는 경우"); //409 confilct

    private final int status;
    private final String code;
    private final String description;

    ErrorCode(int status, String code, String description) {
        this.status = status;
        this.code = code;
        this.description = description;
    }
    public ErrorResponseDto toErrorResponseDto(String msg) {
        return ErrorResponseDto
                .builder()
                .status(this.status)
                .errorCode(this.code)
                .description(this.description)
                .errorMsg(msg)
                .build();
    }
}
```
### 설정 - 환경 변수 
```java
server:
  port: 0

spring:
  application:
    name: auth-service
  jpa:
    show-sql: true
#    hibernate:
#      ddl-auto: create-drop
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://plop-rds.cyjccp4psnuz.ap-northeast-2.rds.amazonaws.com:3306/plop?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&serverTimezone=Asia/Seoul
    username: ${PLOP_DB_USER} # plop-db-user
    password: ${PLOP_DB_PWD} #plop-db-pwd
  redis:
    host: 127.0.0.1
    port: 6379

  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USER}
    password: ${MAIL_PWD}
    properties:
      mail:
        debug: true
        smtp:
          auth: true
          starttls:
            enable: true

eureka:
  instance:
    instance-id: ${spring.cloud.client.hostname}:${spring.application.instance_id:${random.value}}

  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://127.0.0.1:8761/eureka

token:
  # 1시간 개발 편의성을 위해 1분 -> 1시간
  access_expired_time: 3600000
  secret_key: plip
  # 1시간
  refresh_expired_time: 3600000

logging:
  level:
    smilegate.plop.auth: DEBUG
```
![image](https://user-images.githubusercontent.com/78259314/230733712-8b5b8c18-e509-4351-91d3-6736843ccf9e.png)

