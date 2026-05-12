---
description: Değişiklikleri servis bazında gruplayıp conventional commits formatıyla commit at. Push yapmaz.
---

`git-committer` ajanını çağır ve ona mevcut değişiklikleri servis bazında gruplayıp commit'lemesini söyle.

Ajan şu adımları uygulayacak:

1. `git status --short` ile değişen dosyaları listele
2. Dosyaları servis/modül bazında grupla (backend/<servis>, frontend, infrastructure, docs)
3. Her gruptaki değişikliklere bakıp AI slop yorum varsa temizlik önerisi yap
4. Her grup için conventional commits formatında mesaj üret:
   `<type>(<scope>): <açıklama>`
5. Tüm commit planını kullanıcıya göster, onay bekle
6. Onay alınca sırayla commit at — **push yapma**
7. Özet rapor (commit hash'leri + mesajlar)

**Önemli kurallar:**
- `git push` YAPMA (push kullanıcının elinde)
- `git reset`, `git rebase`, `git revert` YAPMA
- Onay olmadan commit ATMA
- Aynı iş birden fazla servisi etkiliyorsa her servis ayrı commit

Kullanıcı "tek tek" derse her commit için ayrı onay al. "hepsi" derse toplu onay yeterli.

Onaysız davranma. Tüm çalışma `git-committer` ajanının system prompt'undaki kurallara uymalı.
