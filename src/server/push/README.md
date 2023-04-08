### 푸시 알림 전송
```java
public void sendByTokenList(RequestMessage message) {
        List<String> tokenList = new ArrayList<>();
        message.getTarget().forEach(target -> {
            UserEntity userEntity = userRepository.findByUserIdOrEmail(target,target);
            String token = userEntity.getFcmToken();
            log.error(userEntity.getUserId() + " : " +token);
            tokenList.add(token);
        });
        List<Message> messages;
        try {
            messages = tokenList.stream().map(token-> Message.builder()
                    .putData("time", LocalDateTime.now().toString())
                    .putData("title", message.getTitle())
                    .putData("body", message.getBody())
                    .putData("roomId", message.getRoomId())
                    .setNotification(new Notification(message.getTitle(),message.getBody()))
                    .setToken(token)
                    .build()).collect(Collectors.toList());
        } catch (NullPointerException e) {
            e.printStackTrace();
            throw new PushFormatException("요청 데이터가 유효하지 않습니다.");
        }

//        MulticastMessage.builder().addAllTokens(tokenList).putData()
        BatchResponse response;
        try {
            //알림 발송
            response = FirebaseMessaging.getInstance().sendAll(messages);
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                List<String> failedTokens = new ArrayList<>();

                for (int i = 0; i< responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        failedTokens.add(tokenList.get(i));
                    }
                    log.error(responses.get(i).getMessageId());
                }
                if (!failedTokens.isEmpty()) {
                    log.error("List of tokens are not valid FCM token : " + failedTokens);
                    throw new PushException("List of tokens are not valid FCM token : " + failedTokens);
                }
            }


        } catch (FirebaseMessagingException e ) {
            log.error("can not send to memberList push message. error info : {}", e.getMessage());
            throw new PushException("can not send to memberList push message. error info : " + e.getMessage());
        }
    }
```
---
### 실제 화면
![image](https://user-images.githubusercontent.com/78259314/230733974-1bbd32c1-589b-4d22-b180-862a979583d4.png)
