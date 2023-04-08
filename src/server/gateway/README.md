### JWT 필터링
```java
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
    Environment env;

    @Autowired
    public AuthorizationHeaderFilter(Environment env) {
        super(Config.class);
        this.env = env;
    }
    public static class Config {

    }
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)){
                throw new JwtTokenMissingException("헤더 없음");
            }

            String authorizationHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            String jwt = authorizationHeader.replace("Bearer ", "");

            log.error("jwt:"+jwt);

            validateJwtToken(jwt);
            return chain.filter(exchange);
        };
    }

    public void validateJwtToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(env.getProperty("token.secret_key"))
                    .parseClaimsJws(token)
                    .getBody();
            log.error(claims.toString());
        } catch (SignatureException | MalformedJwtException |
                 UnsupportedJwtException | IllegalArgumentException | ExpiredJwtException jwtException) {
            jwtException.printStackTrace();
            throw jwtException;
        }
    }

}
```
---
### GlobalExceptionHandler
```java
@Component
@Order(-1) // 내부 bean 보다 우선 순위를 높여 해당 빈이 동작하게 설정
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("In Exception Handler");

        ErrorCode errorCode;
        ErrorResponseDto errorResponseDto;
         if (ex.getClass() == MalformedJwtException.class || ex.getClass() == SignatureException.class
        || ex.getClass() == UnsupportedJwtException.class ) {
             errorCode = ErrorCode.INVALID_TOKEN;
             errorResponseDto = errorCode.toErrorResponseDto("유효하지 않은 토큰");
        } else if (ex.getClass() == ExpiredJwtException.class){
             errorCode = ErrorCode.EXPIRED_TOKEN;
             errorResponseDto = errorCode.toErrorResponseDto("만료된 토큰");
        } else if (ex.getClass() == JwtTokenMissingException.class) {
             errorCode = ErrorCode.MISSING_TOKEN;
             errorResponseDto = errorCode.toErrorResponseDto("토큰이 전달되지 않음");
         } else {
             errorResponseDto = null;
             ex.printStackTrace();
         }
         ObjectMapper mapper = new ObjectMapper();
        String result = null;
        try {
            result = mapper.writeValueAsString(errorResponseDto);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type","application/json");
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Flux.just(buffer));

    }
}
```
---
### 에러코드
```java
public enum ErrorCode {
    INVALID_TOKEN(401, "AUTH-001", "토큰이 유효하지 않은 경우"),
    EXPIRED_TOKEN(401, "AUTH-002", "토큰이 만료된 경우"),
    MISSING_TOKEN(401, "AUTH-003", "토큰을 전달하지 않은 경우");

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
                .errorCode(this.code)
                .status(this.status)
                .description(this.description)
                .errorMsg(msg)
                .build();
    }
}
```
