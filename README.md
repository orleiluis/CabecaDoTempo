# Cabeça do Tempo

App Android para aprender ritmos de guitarra/violão com a **cabeça do tempo bem marcada**.

## O que tem
- **27 ritmos** em 5 grupos: Fundamentos (semínima, colcheia, semicolcheia, contratempo), Pop e Rock (rock, pop/folk, balada, galope, pop dançante, country, blues shuffle), Groove (reggae, ska, funk), Brasileiros (bossa nova, samba, pagode, baião, xote, arrasta-pé, marchinha, vanerão, sertanejo, arrocha) e Ternários/compostos (valsa, guarânia, 6/8).
- **Cabeça do tempo em destaque**: clique forte e agudo no 1, clique médio nas outras cabeças, círculo grande que pisca (vermelho no 1, amarelo nas demais), grade com bordas coloridas nas cabeças e vibração opcional.
- **Violão sintetizado** tocando o padrão de verdade (palhetada ↓↑, abafados, baixo com polegar, dedos pinçando), com a progressão de acordes de cada ritmo.
- **Treino guiado em 5 etapas**: só o clique → ↓ só nas cabeças → 60% → 80% → velocidade alvo.
- BPM com botões, barra e tap tempo; contagem inicial; clique nas subdivisões; acelerar aos poucos (+4 BPM a cada 4 compassos). O BPM de cada ritmo fica salvo.
- Funciona offline, sem login, sem dependências externas. Android 8.0+.

## Gerar o APK
O workflow `.github/workflows/build-apk.yml` compila sozinho a cada push:
1. Suba esta pasta para um repositório no GitHub.
2. Aba **Actions → Gerar APK** → abra a execução → baixe o artefato **CabecaDoTempo-apk**.
3. Descompacte e instale o `CabecaDoTempo.apk` no celular (permitir instalar de fontes desconhecidas).

Também abre direto no Android Studio (`File → Open`).
