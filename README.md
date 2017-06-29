

**GAS meter recognition**
---------------------

1.목적

기존 가스 검침 방식에 불편함을 해소 하고자 가스 계량기의 검침 영역을 자동 객체 검출 하여 바코드 인식(사용자 주소 매칭) 및 가스 사용량을 수치화(가스 사용량 기록) 하는 안드로이드 앱 모듈 개발. 

2.개발 범위

 - 자동 객체 인식 : OpenCV를 이용한 계량기 객체 트레이닝, 및 검출 (Android NDK로 구현)
 - 바코드 인식 : ZXing 라이브러리를 통한 바코드 영역 검출 및 바코드 스캔 (Android) 
 - 검출된 영역을 이미지 저장 및 전송 (Android)
 - 저장된 이미지의 영역에서 숫자 검침 역역 분리(Python)
 - 숫자 검침 영역 전처리(Python)
 - tesseract를 이용한 숫자 트레이닝 및 인식
 - 검출영역 이미지 전송 IOT 프로토 타입 장치 개발(Arduino 이용한 이미지 전송)

3.구현 사항
   
 - Andorid APP
      객체검출(OpenCV 이용)
      바코드인식(Zxing 이용) 
 - 서버
	  
	  
4.구현 결과
	
 - Yesco 계량기 객체 트레이닝 
 - Yesco 계량기 객체 객체 검출 성공


[![Watch the video](http://reebot.io:8083/images/gitimage/intro_grs_img.png)](https://youtu.be/5mNENdAQnIQ)
