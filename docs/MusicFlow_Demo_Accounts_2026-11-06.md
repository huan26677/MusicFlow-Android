# MusicFlow — Tai khoan Supabase QA / Demo (2026-11-06)

## Pham vi

- Supabase project: `vzvxpbwrftlnyvuhfoqn` (hosted, khong phai local fake data).
- Da tao **8 tai khoan QA** bang Supabase Auth signup API; ca 8 deu duoc xac nhan **password sign-in PASS** ngay khi tao.
- **Owner** hien co (`BossHuan26677`, UUID `a20fb14d-31ef-4ef3-bc72-ecbb8c4bff78`) duoc giu nguyen. Khong tao Owner thu hai, khong thay doi mat khau Owner.
- Du lieu chi la fixture demo, khong thanh toan, khong upload nhac hay cap giay phep streaming.

## Tai khoan / phan quyen tren backend

| Persona | Supabase email | Backend status |
|---|---|---|
| Free | `musicflow.demo.free.20261106@example.com` | Free mac dinh, khong co elevated role |
| Premium | `musicflow.demo.premium.20261106@example.com` | `account_entitlements.tier=premium`, `source=promotion`, het han 2027-11-06 UTC |
| PlatformAdmin | `musicflow.demo.admin.20261106@example.com` | role `platform_admin`; `manage_users`, `review_creators`, `moderate_catalog` |
| Developer | `musicflow.demo.developer.20261106@example.com` | role `developer`; shortcut chi tren Android DEBUG |
| Moderator | `musicflow.demo.moderator.20261106@example.com` | role `moderator`, chua co API moderation rieng |
| Artist | `musicflow.demo.artist.20261106@example.com` | role `artist`, application `approved`, creator profile mau |
| Author | `musicflow.demo.author.20261106@example.com` | role `author`, application `approved`, creator profile mau |
| Creator pending | `musicflow.demo.pending.20261106@example.com` | Free, application artist `pending`, chua co creator role |
| Guest | (khong co account) | Che do guest/offline trong ung dung |

## Cach thuc va xac minh

- Account signup va password login: **8/8 PASS** tai thoi diem tao.
- DB da xac minh **8/8** Auth users/profile demo, `platform_admin` co 3 permissions, `artist`/`author` co role, application `approved` va profile, `pending` con cho duyet, Free khong co premium, Premium co entitlement den 2027-11-06.
- Qua trinh seed role/permissions duoc thuc hien boi trusted database administrative connection. `role_audit.actor_id=null` va action `demo_fixture_*` phan biet voi thao tac Owner tren giao dien. Approved creator applications la **QA simulation**, khong phai review ban quyen that.
- Chua chay **post-seed** Android UI / REST RPC theo tung role (lenh dung lai session/mat khau tren may dev bi protection guard chan); khong duoc ghi PASS cho nhung phan chua chay.
- Cac role khong thay doi RLS, service key, Auth provider, billing hay phat hanh Play Store.

## Thong tin dang nhap va tai su dung

- Mat khau QA duoc tao ngau nhien rieng moi account, **khong ghi vao repo Git / tai lieu tracked**.
- Mat khau duoc ban giao trong cau tra loi tao account cho chu du an; can cat vao password manager cua chu du an. Khong doc mat khau Owner tu file local.
- `example.com` la domain danh cho mau; khong co hop thu de nhan email reset password. Neu mat mat khau QA can reset qua Supabase Dashboard voi quyen quan tri, hoac xoa/recreate tai khoan QA.
- Khong duoc dung cac account demo nay cho production; truoc release can vo hieu hoa/xoa tai khoan QA, danh gia lai email confirmation va policy.

## Kiem thu tiep theo

1. Android login/logout va role badge bang 8 credentials; `musicflow_my_access` phai match DB.
2. Web admin: PlatformAdmin tim users va duyet pending creator; Developer/Free phai bi deny.
3. Premium vs Free entitlement; Developer shortcut chi hien tren `BuildConfig.DEBUG`.
4. Neu de demo quy trinh pending, dung creator pending va sau do reset/tao lai pending request khi can quay lai trang thai ban dau.
