# 스마일게이트 윈터데브캠프 2023 - Plop
> #### MSA기반 채팅 애플리케이션
> #### 맡은 파트(BE) : 서비스 디스커버리, 게이트웨이, 인증 서버, 유저 서버, 푸시 서버
> ##### 팀원 : [김가희(AOS)](https://github.com/kimgaheeme)&ensp;[김호준(IOS)](https://github.com/elddy0948)&ensp;[김주현(BE)](https://github.com/llsrrll96) &ensp;[이제호(BE)](https://github.com/jhl8109)
###### 현재 페이지는 [이제호](https://github.com/jhl8109)가 맡은 파트를 중심으로 기록하였습니다.

### 목차
[1. 목표](#목표)<br>
[2. 주요 기능](#주요-기능)<br>
[3. 사용 기술](#사용-기술)<br>
[4. 아키텍처](#아키텍처)<br>
[5. 상세 기능](#상세-기능)<br>
[6. 서버별 주요 코드](#서버별-주요-코드)<br>
[7. 회고](#회고)<br>
[8. Docs](#docs)<br><br>

## 목표
<p align="center"><img width="800" alt="스크린샷 2023-04-09 오전 1 30 07" src="https://user-images.githubusercontent.com/78259314/230732350-6e54e7f4-1720-42f3-813b-ab403289e725.png"></p>

## 주요 기능
<p align="center"><img width="800" alt="스크린샷 2023-04-09 오전 1 29 29" src="https://user-images.githubusercontent.com/78259314/230732323-b19e5e28-e0b1-42e3-b293-ab54d082007e.png"></p>
<br>

#### 짧은 시연 영상 (유튜브 링크, 1분 56초)
[![Video](https://user-images.githubusercontent.com/78259314/230733200-3fe3eac4-bf42-4095-92c3-aa03f676e61c.png)](https://youtu.be/CHYVsNMhxLk?t=0s)<br>

## 사용 기술
- 백엔드 : Spring Boot, Spring Cloud(Eureka, Gateway)
- 데이터베이스 : MySQL
- 캐시 : Redis
- 배포 : Docker, AWS EC2, AWS S3
- 푸시 알림 : FCM
<br>

## 아키텍처
|상세 아키텍처|배포 아키텍처|
|---|---|
|<img alt="스크린샷 2023-04-09 오전 1 36 21" src="https://user-images.githubusercontent.com/78259314/230732623-49a5034e-20e2-4d3d-8ca1-0b6f5128feef.png">|<img alt="스크린샷 2023-04-09 오전 1 35 53" src="https://user-images.githubusercontent.com/78259314/230732608-1d69e8c8-2004-4750-8688-2c7278cfb779.png">|
<br>

## 상세 기능
|유저 & 인증|푸시 알림|
|---|---|
|<img src=https://user-images.githubusercontent.com/78259314/230734067-6e7064d4-d020-4bb7-b68e-252a29aa1074.png>|<img src=https://user-images.githubusercontent.com/78259314/230734053-10f28313-71df-4aa5-a1d2-1054bada8622.png>
<br>

## 서버별 주요 코드
- [게이트웨이](https://github.com/jhl8109/smilegate-winterdevcamp-plop/tree/main/src/server/gateway)
- [인증 서버](https://github.com/jhl8109/smilegate-winterdevcamp-plop/new/main/src/server/auth)
- [유저 서버](https://github.com/jhl8109/smilegate-winterdevcamp-plop/tree/main/src/server/user)
- [푸시 서버](https://github.com/jhl8109/smilegate-winterdevcamp-plop/tree/main/src/server/push)
<br>

## 회고


## Docs
- [PMP](https://github.com/jhl8109/smilegate-winterdevcamp-plop/blob/main/docs/%EA%B2%BD%EB%82%A8_Plop_PMP.pdf)
- [아키텍처](https://github.com/jhl8109/smilegate-winterdevcamp-plop/blob/main/docs/%EA%B2%BD%EB%82%A8%20Plop%20%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98%20%EB%A6%AC%EB%B7%B0_20230113.pdf)
- [중간발표](https://github.com/jhl8109/smilegate-winterdevcamp-plop/blob/main/docs/%EC%9C%88%ED%84%B0%EB%8D%B0%EB%B8%8C%EC%BA%A0%ED%94%84_%EA%B2%BD%EB%82%A8%EC%A7%80%EB%B6%80_PLOP_%EC%A4%91%EA%B0%84%EB%B0%9C%ED%91%9C.pdf)
- [최종발표](https://github.com/jhl8109/smilegate-winterdevcamp-plop/blob/main/docs/%5B%E1%84%8E%E1%85%AC%E1%84%8C%E1%85%A9%E1%86%BC%E1%84%87%E1%85%A1%E1%86%AF%E1%84%91%E1%85%AD%5D%20%E1%84%80%E1%85%A7%E1%86%BC%E1%84%82%E1%85%A1%E1%86%B7_Plop.pdf)








