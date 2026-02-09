# MCP Block Mod

Um mod para Minecraft que adiciona um bloco interativo que permite interagir com um modelo de linguagem (LLM) diretamente no jogo.

## Recursos

- Bloco interativo com GUI personalizada
- Comandos seguros executados via LLM:
  - `/give` - Dar itens ao jogador
  - `/summon` - Invocar entidades
- Processamento de coordenadas relativas (~) para comandos de summon
- Integração com LMStudio (servidor local LLM)
- Verificação de segurança com whitelist de comandos
- Logging detalhado para depuração

## Instalação

### Requisitos do Servidor Fabric
Para o servidor Fabric funcionar corretamente, é necessário instalar os seguintes mods na pasta `mods`:

1. **balm-fabric-1.21.11-21.11.6.jar** - Biblioteca para mods Fabric
2. **craftingtweaks-fabric-1.21.11-21.11.4.jar** - Ferramentas de crafting
3. **fabric-api-0.141.3+1.21.11.jar** - API essencial do Fabric
4. **jei-1.21.11-fabric-27.4.0.15.jar** - Interface de informação de itens (opcional, mas recomendado)
5. **mcpblock-1.0.0.jar** - Mod MCP Block (arquivo buildado do projeto)

### Passo a Passo para Instalação
1. Certifique-se de ter o Minecraft Fabric 1.21.11 instalado
2. Instale o Java 21 ou superior
3. Baixe todos os mods listados acima
4. Coloque os arquivos `.jar` na pasta `mods` do seu servidor Fabric
5. Inicie o servidor

## Uso

1. Coloque o bloco MCP no mundo (/give @s mcpblock:mcp_block) obs.: para criar blocos precisa de permissão no servidor (op {nick_player})
2. Clique com o botão direito no bloco para abrir a GUI
3. Digite sua pergunta ou solicitação (ex: "summon ender dragon" ou "give me diamond sword")
4. Clique em enviar
5. O LLM processará sua solicitação e executará o comando se for seguro

## Arquitetura do Mod

### Estrutura de Arquivos
```
com.seumod.mcpblock/
├── block/
│   └── McpBlock.java - Definição do bloco interativo
├── client/
│   ├── McpClient.java - Initialização do cliente
│   └── gui/
│       ├── McpGuiMod.java - Gerenciamento da GUI
│       └── McpScreen.java - Implementação da tela
├── network/
│   ├── McpPackets.java - Gerenciamento de pacotes de rede
│   ├── McpRequestPayload.java - Payload da requisição
│   └── McpResponsePayload.java - Payload da resposta
└── service/
    ├── McpHttpClient.java - Cliente HTTP para API
    └── McpHttpClientV2.java - Cliente HTTP para LMStudio
```

### Comunicação com LMStudio

O mod se comunica com o LMStudio rodando localmente na porta 1234. Certifique-se de que o LMStudio está executando e a API está acessível.

## Configuração

### Arquivo fabric.mod.json

```json
{
  "schemaVersion": 1,
  "id": "mcpblock",
  "version": "1.0.0",
  "name": "MCP Block",
  "description": "Block for interacting with LLM in Minecraft",
  "authors": [
    "Lucas Guilha"
  ],
  "environment": "*",
  "entrypoints": {
    "main": [
      "com.seumod.mcpblock.McpBlockMod"
    ],
    "client": [
      "com.seumod.mcpblock.McpBlockModClient"
    ]
  },
  "depends": {
    "fabricloader": ">=0.18.4",
    "fabric": ">=0.141.3",
    "minecraft": "1.21.11"
  }
}
```

## Troubleshooting

### Problemas Comuns

1. **Comando não executado**: Verifique se o comando está na whitelist (give, summon)
2. **Erro de entidade não encontrada**: Certifique-se de que o nome da entidade é válido (ex: minecraft:ender_dragon)
3. **LMStudio não responde**: Verifique se o LMStudio está rodando na porta 1234
4. **Falha na conexão**: Verifique se você está usando o Minecraft 1.21.11 com Fabric e Java 21

## Desenvolvimento

### Requisitos Prévios
- Java 21 ou superior
- Git
- Fabric Development Environment para Minecraft 1.21.11

### Compilação e Build

1. **Clonar o repositório:**
   ```bash
   git clone https://github.com/medowlucas/minecraft-AI-block-mod.git
   ```

2. **Compilar o mod:**
   ```bash
   # Windows
   .\gradlew.bat build

   # Linux/Mac
   ./gradlew build
   ```
   O arquivo `.jar` final estará em `build/libs/`

3. **Executar cliente em modo debug:**
   ```bash
   # Windows
   .\gradlew.bat runClient

   # Linux/Mac
   ./gradlew runClient
   ```

5. **Limpar build antigo:**
   ```bash
   # Windows
   .\gradlew.bat clean

   # Linux/Mac
   ./gradlew clean
   ```

### Configuração do LMStudio

1. Instale o LMStudio (https://lmstudio.ai/)
2. Baixe um modelo (ex: Llama 3 ou Mistral)
3. Inicie o servidor LMStudio na porta 1234
4. Certifique-se de que a API está acessível em `http://localhost:1234`

## Segurança

O mod implementa medidas de segurança para bloquear comandos perigosos:
- Whitelist de comandos permitidos
- Verificação de nomes de jogador explicitos para /give
- Bloqueio de seletores (@p, @a, @e, etc.)
- Validação de padrões de comando

## Contribuição

Sinta-se à vontade para abrir issues e pull requests no repositório do GitHub.
