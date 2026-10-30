#!/usr/bin/env python3
"""Interactive terminal menu for the three server installation stages."""
import os
import pathlib
import subprocess
import sys
import playit
import server_config

ROOT = pathlib.Path(__file__).resolve().parents[1]
COLOR = sys.stdout.isatty() and not os.environ.get('NO_COLOR') and os.environ.get('TERM') != 'dumb'


def paint(text, color='35'):
    return '\033['+color+'m'+text+'\033[0m' if COLOR else text


def playit_state():
    try:
        return playit.check_ready()
    except (OSError, RuntimeError, ValueError):
        return None


def installed():
    return (ROOT/'server/server.jar').is_file() and ((ROOT/'server/plugins/TreinoHub-1.0.0.jar').is_file() or (ROOT/'server/.bwfe-template.json').is_file()) and (ROOT/'server/server.properties').is_file()


def dashboard():
    ready = playit_state()
    configured = 'concluído' if ready else 'pendente'
    available = 'instalado' if installed() else 'pendente'
    print('\n'+paint('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'))
    print(paint('  RYUEX TREINO  •  INSTALAÇÃO E CONTROLE', '1;35'))
    print('  Minecraft Java 1.8.9 | original + não original')
    print(paint('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'))
    print(f"  1  {'Configurar Playit':<35}[{configured}]")
    print(f"  2  {'Instalar servidor completo':<35}[{available}]")
    print('  3  Ligar servidor e mostrar IP/porta')
    print('  4  Ver endereço salvo e configuração')
    print('  5  Atualizar servidor (preservar dados)')
    print('  6  Parar servidor com segurança')
    print('  7  Importar/verificar novo template BWFE')
    print('  0  Sair')
    print(paint('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'))
    if ready:
        info = playit.connection_info(ready)
        print('  Endereço salvo: '+info['connect'])
    print('  Ordem inicial: 1 → 2 → 3\n')


def call(args, environment=None):
    result = subprocess.run(args, cwd=ROOT, env=environment)
    if result.returncode:
        print(paint(f'\nEtapa não concluída (código {result.returncode}). Corrija a mensagem acima e tente novamente.', '33'))
        return False
    print(paint('\nEtapa concluída.', '32'))
    return True


def configure():
    print('\nPASSO 1 — CONFIGURAR PLAYIT\nVincule sua conta no link mostrado e cadastre o túnel Minecraft Java.\n')
    call([sys.executable, str(ROOT/'scripts/playit.py'), 'setup'])


def install():
    if not playit_state():
        print('Conclua primeiro a opção 1: configurar Playit.')
        return
    print('\nPASSO 2 — INSTALAR SERVIDOR COMPLETO')
    print('Prepara o template BWFE com arenas e verifica as dependências.')
    print('Coloque o ZIP BWFE em imports/ se ainda não importou.\n')
    call(['bash', str(ROOT/'atualizar.sh')])


def update_server():
    print('\nATUALIZAR SERVIDOR — solicita parada segura nas sessões compatíveis.')
    print('Prepara BWFE em separado, verifica requisitos e preserva backup antes da troca.\n')
    if call(['bash', str(ROOT/'atualizar.sh')]):
        print('Reabrindo o menu atualizado...', flush=True)
        os.execv(sys.executable, [sys.executable, str(ROOT/'scripts/menu.py')])


def ensure_eula():
    path = ROOT/'server/eula.txt'
    if path.exists() and server_config.entries(path.read_text()).get('eula') == 'true':
        return True
    print('\nLeia a EULA do Minecraft: https://aka.ms/MinecraftEULA')
    reply = input('Se concordar, digite ACEITO para iniciar: ').strip().upper()
    if reply != 'ACEITO':
        print('Inicialização cancelada. A EULA continua sem aceite.')
        return False
    return True


def start():
    data = playit_state()
    if not data:
        print('Conclua primeiro a opção 1: configurar Playit.')
        return
    if not installed():
        print('Conclua primeiro a opção 2: instalar servidor.')
        return
    try:
        values = server_config.check()
    except (OSError, RuntimeError) as error:
        print(str(error))
        return
    if not ensure_eula():
        return
    print('\nPASSO 3 — LIGAR SERVIDOR\n')
    playit.print_connection(data)
    print('Aguarde Done no console para entrar. Digite stop para desligar e voltar ao menu.\n')
    if values.get('white-list') == 'true':
        print('Whitelist ativa nesta instalação: adicione os nicks permitidos no console.')
    environment = os.environ.copy()
    environment['ACCEPT_EULA'] = 'true'
    call(['bash', str(ROOT/'scripts/start.sh')], environment)


def details():
    data = playit_state()
    if not data:
        print('Nenhum endereço configurado. Use a opção 1.')
        return
    print('\nENDEREÇO CADASTRADO — a exibição não indica que o servidor esteja online.\n')
    playit.print_connection(data)
    print('Cliente: Minecraft Java 1.8.9 | BWFE: PandaSpigot 1.8.8 / Java 17')
    print('Modo configurado pelo instalador: online-mode=false (originais e não originais).')
    print('O Playit deve ficar ativo junto com o Minecraft para aceitar conexões públicas.')


def template_menu():
    stage = ROOT/'template-server'
    if stage.exists():
        call([sys.executable, str(ROOT/'scripts/template.py'), 'check'])
    else:
        archive = input('Caminho do ZIP BWFE 25.1.0: ').strip()
        if archive:
            call([sys.executable, str(ROOT/'scripts/template.py'), 'import', archive])
    print('A importação prepara uma instalação separada. Consulte docs/MIGRACAO-BWFE.md.')


def main():
    actions = {'7': template_menu, '1': configure, '2': install, '3': start, '4': details, '5': update_server, '6': lambda: call(['bash', str(ROOT/'scripts/stop.sh')])}
    while True:
        try:
            dashboard()
            choice = input('Escolha uma opção: ').strip()
            if choice == '0':
                print('Menu encerrado.')
                return 0
            if choice not in actions:
                print('Opção inválida. Use 1, 2, 3, 4, 5, 6, 7 ou 0.')
                continue
            actions[choice]()
        except EOFError:
            print('\nEntrada encerrada. Execute bash menu.sh em um terminal interativo.')
            return 0
        except KeyboardInterrupt:
            print('\nOperação interrompida. Voltando ao menu.')
        except (OSError, ValueError, RuntimeError, subprocess.SubprocessError) as error:
            print('Erro: '+str(error))


if __name__ == '__main__':
    raise SystemExit(main())
