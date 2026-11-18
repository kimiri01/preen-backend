# DB 마이그레이션(Flyway) 팀 규칙

이 프로젝트는 테이블 구조를 **Flyway SQL 파일로만** 바꿉니다.
(`spring.jpa.hibernate.ddl-auto=validate` 이라서 Entity만 바꾸면 서버가 켜지지 않습니다.)

SQL 파일 위치: `src/main/resources/db/migration`

## 지금 들어 있는 파일

| 파일 | 내용 |
| --- | --- |
| `V1__user_auth_terms.sql` | 회원·인증·약관 테이블 |
| `V2__host_accommodation.sql` ~ `V7__review_music_only.sql` | 호스트·숙소·재고·예약·결제·리뷰·찜·카테고리 |
| `V100__sample_accommodations_rooms.sql`, `V101__...sql` | 숙소·카테고리·지역 샘플 데이터 |
| `V102__auth_signup_email_verification.sql` | 회원가입·이메일 인증에 맞게 회원 테이블 수정 + 약관 초안 |

## 새 DB 기능을 추가할 때 (모든 팀원 공통)

1. **최신 develop에서 브랜치를 만듭니다.**
   ```bash
   git switch develop
   git pull
   git switch -c feature/내기능_이름
   ```
2. **새 SQL 파일을 추가합니다.** 파일 이름은 `V날짜.시분__설명.sql` 형식으로 합니다.
   ```text
   V20261120.1430__add_reservation_memo.sql
   ```
   - 날짜+시간으로 번호를 붙이면 팀원끼리 번호가 겹치지 않습니다.
   - 이 번호는 기존 `V102`보다 크기 때문에 항상 기존 파일 뒤에 실행됩니다.
   - `__`(언더바 2개)를 꼭 넣습니다.
3. **이미 develop에 올라간 SQL 파일은 절대 수정하지 않습니다.**
   바꿀 내용이 있으면 `ALTER TABLE ...` 이 들어간 새 파일을 추가합니다.
   (수정하면 다른 팀원 서버가 `checksum mismatch` 에러로 켜지지 않습니다.)
4. 이미 있는 테이블은 `CREATE TABLE`로 다시 만들지 않습니다. 필요한 컬럼만 `ALTER TABLE`로 추가합니다.
5. Entity(`@Entity`)도 SQL과 같은 컬럼 이름·타입으로 맞춥니다.

## 서버가 Flyway 에러로 안 켜질 때 (로컬 개발 DB 한정)

MySQL Workbench에서 DB를 새로 만들고 서버를 다시 켜면 V1부터 다시 만들어집니다.

```sql
DROP DATABASE preen;
CREATE DATABASE preen;
```

⚠️ 팀 공용 DB나 배포 서버 DB에서는 절대 하지 않습니다.

## 로그인 없이 열어야 하는 API를 만들 때

`common/security/SecurityRuleContributor`를 구현한 `@Component`를 기능 패키지에 하나 만듭니다.
`SecurityConfig`는 고칠 필요가 없습니다. (예: `accommodation/config/AccommodationSecurityRules.java`)
