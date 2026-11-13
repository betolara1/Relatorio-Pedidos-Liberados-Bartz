# 📦 Consulta de Pedidos Liberados

Aplicativo desktop desenvolvido em **Java (Swing)** para consulta, filtragem e impressão de pedidos liberados integrados ao banco de dados **IBM DB2**.

---

## 🚀 Funcionalidades

- **Consulta por Período de Datas:**
  - Seleção intuitiva de data inicial e data final via calendário/setinhas ou digitação direta.
  - Validação estrita: a data final não pode ser menor que a data inicial.
  - Sincronização automática entre seletores para evitar períodos inconsistentes.
  - Formatação amigável das datas no padrão brasileiro (`dd/MM/yyyy`).

- **Consulta Específica por Nº do Pedido:**
  - Campo dedicado para busca rápida pelo número do pedido (usando índice no DB2).
  - **Filtro reativo:** ao digitar o número do pedido, os campos de data são automaticamente desabilitados, focando a pesquisa exclusivamente no pedido.
  - Atalho de teclado: pressione **`Enter`** no campo para pesquisar imediatamente.
  - Botão **Limpar** para resetar filtros e reativar os seletores de datas.

- **Visualização em Tabela:**
  - Exibição em grade com colunas centralizadas (**Nº Pedido** e **Data de liberação**).
  - Status em tempo real no rodapé com contagem de pedidos encontrados.

- **Impressão e Exportação para PDF:**
  - Botão **Imprimir** integrado com a caixa de diálogo nativa do Windows.
  - Ajuste inteligente na largura da folha (`FIT_WIDTH` em A4/Carta).
  - Cabeçalho dinâmico com o período ou número do pedido consultado.
  - Rodapé com numeração de página e total de pedidos.
  - Permite imprimir em qualquer impressora física ou salvar direto como **PDF** via *"Microsoft Print to PDF"*.

- **Portabilidade Total (Não precisa de Java no cliente):**
  - O aplicativo pode ser empacotado com o **Java embutido** via `jpackage`.
  - Funciona em qualquer máquina Windows (Windows 10, 11 e Server) sem necessidade de instalar JDK ou JRE.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** Java 21 LTS
- **Interface Gráfica:** Java Swing
- **Banco de Dados:** IBM DB2
- **Driver JDBC:** IBM DB2 JCC (`com.ibm.db2:jcc:11.5.9.0`)
- **Gerenciador de Dependências:** Apache Maven
- **Plugins Maven:**
  - `maven-compiler-plugin` (Java 21)
  - `exec-maven-plugin` (execução em desenvolvimento)
  - `maven-shade-plugin` (empacotamento de Fat JAR com todas as dependências)
- **Empacotador:** `jpackage` (geração de `.exe` nativo para Windows)

---

## 📁 Estrutura do Projeto

```plaintext
Formularios-Pedidos-Liberados/
├── src/
│   └── main/
│       └── java/
│           └── br/
│               └── com/
│                   └── bartz/
│                       └── pedidos/
│                           └── PedidosLiberadosApp.java   # Código-fonte principal da aplicação
├── .env.example                                          # Modelo de configuração de ambiente
├── .gitignore                                            # Arquivos ignorados pelo Git
├── gerar-exe.bat                                         # Script automatizado para gerar .exe e .zip
├── pom.xml                                               # Configuração do Maven e dependências
└── README.md                                             # Documentação do projeto
```

---

## ⚙️ Configuração do Ambiente

1. Clone ou baixe este repositório.
2. Crie o arquivo `.env` na raiz do projeto com base no arquivo [.env.example](file:///.env.example):

```properties
URL_DB2=jdbc:db2://localhost:50000/BANCO_DE_DADOS
DB2_USER=seu_usuario
DB2_PASS=sua_senha_aqui
```

> **Nota:** Se o arquivo `.env` não for fornecido, a aplicação utilizará os parâmetros de conexão padrões configurados no código.

---

## 💻 Como Executar em Desenvolvimento

Certifique-se de ter o **JDK 21** e o **Maven** instalados no seu computador.

Abra o terminal na pasta do projeto e execute:

```powershell
mvn compile exec:java
```

---

## 📦 Como Gerar o Executável (.exe) para Distribuição

### Método Automático (Recomendado):

Basta dar dois cliques no arquivo:
👉 **`gerar-exe.bat`**

O script executará automaticamente:
1. Compilação do código e empacotamento do JAR com as dependências do DB2.
2. Geração da pasta com o executável e o Java embutido (`dist/PedidosLiberados/`).
3. Cópia do arquivo `.env`.
4. Compactação automática da pasta em um arquivo pronto para envio: **`dist/PedidosLiberados.zip`**.

### Método Manual:

```powershell
# 1. Compilar e empacotar o JAR com dependências
mvn clean package

# 2. Gerar o executável nativo com runtime embutido
jpackage --type app-image --name "PedidosLiberados" --input target --main-jar pedidos-liberados-1.0.0.jar --main-class br.com.bartz.pedidos.PedidosLiberadosApp --dest dist

# 3. Copiar o arquivo de configuração
copy .env dist\PedidosLiberados\.env

# 4. Compactar em ZIP para envio
powershell -Command "Compress-Archive -Path dist\PedidosLiberados -DestinationPath dist\PedidosLiberados.zip -Force"
```

---

## 📲 Como Enviar para o Cliente / Usuário Final

1. Envie o arquivo **`dist/PedidosLiberados.zip`** gerado (via WhatsApp, e-mail, rede ou pendrive).
2. O usuário no destino deve:
   1. Baixar o arquivo `PedidosLiberados.zip`.
   2. Clicar com o botão direito e selecionar **"Extrair Tudo..."**.
   3. Abrir a pasta extraída e executar **`PedidosLiberados.exe`**.

> [!IMPORTANT]
> Nunca envie apenas o arquivo `.exe` solto fora da pasta, pois ele precisa dos diretórios de suporte (`runtime/` e `app/`) que vão dentro do `.zip`.

---

## 📄 Licença

Uso interno desenvolvido para **Bartz Móveis Planejados**.

