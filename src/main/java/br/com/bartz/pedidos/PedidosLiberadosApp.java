package br.com.bartz.pedidos;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Programa com interface gráfica (Swing) para consultar no DB2
 * quais pedidos foram liberados dentro de um intervalo de datas
 * ou pesquisar por um número de pedido específico.
 */
public class PedidosLiberadosApp extends JFrame {

    // ------------------------------------------------------------------
    // CONFIGURAÇÃO DA CONEXÃO COM O BANCO DE DADOS
    // Valores carregados estritamente do arquivo .env ou variáveis de ambiente.
    // Nenhum IP, usuário ou senha fica fixado no código Java.
    // ------------------------------------------------------------------
    private static final String DB_URL  = obterConfig("URL_DB2", "DB2_URL");
    private static final String DB_USER = obterConfig("DB2_USER");
    private static final String DB_PASS = obterConfig("DB2_PASS");

    /**
     * Busca uma configuração exclusivamente nas variáveis de ambiente ou no arquivo .env.
     */
    private static String obterConfig(String chave, String... alternativas) {
        String val = System.getenv(chave);
        if (val != null && !val.isBlank()) return val.trim();

        for (String alt : alternativas) {
            val = System.getenv(alt);
            if (val != null && !val.isBlank()) return val.trim();
        }

        // Tenta ler do arquivo .env
        java.io.File envFile = new java.io.File(".env");
        if (envFile.exists()) {
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.FileReader(envFile, java.nio.charset.StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = br.readLine()) != null) {
                    linha = linha.trim();
                    if (linha.isEmpty() || linha.startsWith("#") || !linha.contains("=")) continue;
                    String[] partes = linha.split("=", 2);
                    String k = partes[0].trim();
                    String v = partes[1].trim();
                    if (k.equalsIgnoreCase(chave)) return v;
                    for (String alt : alternativas) {
                        if (k.equalsIgnoreCase(alt)) return v;
                    }
                }
            } catch (Exception ignored) {}
        }

        return "";
    }

    /**
     * Garante que os dados de conexão foram informados no .env antes de tentar conectar.
     */
    private static void validarConfiguracoes() {
        if (DB_URL == null || DB_URL.isBlank()) {
            throw new IllegalStateException("Configuração 'URL_DB2' não foi encontrada no arquivo .env.");
        }
        if (DB_USER == null || DB_USER.isBlank()) {
            throw new IllegalStateException("Configuração 'DB2_USER' não foi encontrada no arquivo .env.");
        }
        if (DB_PASS == null || DB_PASS.isBlank()) {
            throw new IllegalStateException("Configuração 'DB2_PASS' não foi encontrada no arquivo .env.");
        }
    }

    // ------------------------------------------------------------------
    // CONSULTAS SQL
    // ------------------------------------------------------------------
    private static final String SQL_POR_DATA =
            "SELECT NRO_PEDIDO, DT_LIBERACAO " +
            "FROM pedido " +
            "WHERE DT_LIBERACAO >= ? AND DT_LIBERACAO < ? " +
            "ORDER BY DT_LIBERACAO, NRO_PEDIDO";

    private static final String SQL_POR_PEDIDO =
            "SELECT NRO_PEDIDO, DT_LIBERACAO " +
            "FROM pedido " +
            "WHERE NRO_PEDIDO = ? " +
            "ORDER BY DT_LIBERACAO, NRO_PEDIDO";

    // Formato usado para mostrar datas na tela (apenas dia/mês/ano)
    private static final DateTimeFormatter FORMATO_TELA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ------------------------------------------------------------------
    // COMPONENTES VISUAIS (campos da tela)
    // ------------------------------------------------------------------
    private final JTextField campoPedido       = new JTextField(8);
    private final JLabel     labelPedido       = new JLabel("Nº Pedido:");

    private final JLabel     labelDataInicial  = new JLabel("Data inicial:");
    private final JSpinner   spinnerInicio     = criarSeletorDeData();
    private final JLabel     labelDataFinal    = new JLabel("Data final:");
    private final JSpinner   spinnerFim        = criarSeletorDeData();

    private final JButton    botaoPesquisar    = new JButton("Pesquisar");
    private final JButton    botaoLimpar       = new JButton("Limpar");
    private final JButton    botaoImprimir     = new JButton("Imprimir");
    private final JLabel     labelStatus       = new JLabel("Escolha o período ou digite o número do pedido e clique em Pesquisar.");

    // Modelo da tabela: define as colunas e guarda as linhas exibidas
    private final DefaultTableModel modeloTabela = new DefaultTableModel(
            new String[]{"Nº Pedido", "Data de liberação"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // impede o usuário de editar as células
        }
    };

    // Componente da tabela
    private final JTable tabela = new JTable(modeloTabela);

    /** Construtor: monta a janela e organiza os componentes. */
    public PedidosLiberadosApp() {
        super("Pedidos liberados por período");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // ----- Painel superior de filtros -----
        JPanel painelFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        painelFiltro.add(labelPedido);
        painelFiltro.add(campoPedido);

        // Separador vertical visual
        JSeparator separador = new JSeparator(SwingConstants.VERTICAL);
        separador.setPreferredSize(new Dimension(2, 24));
        painelFiltro.add(separador);

        painelFiltro.add(labelDataInicial);
        painelFiltro.add(spinnerInicio);
        painelFiltro.add(labelDataFinal);
        painelFiltro.add(spinnerFim);
        painelFiltro.add(botaoPesquisar);
        painelFiltro.add(botaoLimpar);
        painelFiltro.add(botaoImprimir);
        add(painelFiltro, BorderLayout.NORTH);

        // ----- Centro: tabela com rolagem para exibir os resultados -----
        tabela.setRowHeight(24);

        // Centraliza o conteúdo das colunas (Nº Pedido e Data)
        DefaultTableCellRenderer renderCentralizado = new DefaultTableCellRenderer();
        renderCentralizado.setHorizontalAlignment(SwingConstants.CENTER);
        tabela.getColumnModel().getColumn(0).setCellRenderer(renderCentralizado);
        tabela.getColumnModel().getColumn(1).setCellRenderer(renderCentralizado);

        add(new JScrollPane(tabela), BorderLayout.CENTER);

        // ----- Rodapé: mensagem de status (quantidade de pedidos, erros etc.) -----
        labelStatus.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(labelStatus, BorderLayout.SOUTH);

        // Ações dos botões e campos
        botaoPesquisar.addActionListener(e -> pesquisar());
        campoPedido.addActionListener(e -> pesquisar());
        botaoLimpar.addActionListener(e -> limparFiltros());
        botaoImprimir.addActionListener(e -> imprimir());

        // Ao digitar no campo de pedido, desativa a busca por data
        campoPedido.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { atualizarModoPesquisa(); }
            @Override public void removeUpdate(DocumentEvent e) { atualizarModoPesquisa(); }
            @Override public void changedUpdate(DocumentEvent e) { atualizarModoPesquisa(); }
        });

        // Sincronização inteligente dos seletores: se início for avançado além do fim, o fim avança junto
        spinnerInicio.addChangeListener(e -> {
            try {
                spinnerInicio.commitEdit();
                spinnerFim.commitEdit();
            } catch (Exception ignored) {}
            java.util.Date dIni = (java.util.Date) spinnerInicio.getValue();
            java.util.Date dFim = (java.util.Date) spinnerFim.getValue();
            if (dFim != null && dIni != null && dFim.before(dIni)) {
                spinnerFim.setValue(dIni);
            }
        });

        // Tamanho da janela e posição centralizada na tela
        setSize(860, 520);
        setLocationRelativeTo(null);
    }

    /**
     * Quando há um número de pedido digitado, desabilita os seletores de data
     * para que a busca seja realizada pelo número do pedido.
     */
    private void atualizarModoPesquisa() {
        boolean temPedido = !campoPedido.getText().trim().isEmpty();
        boolean habilitarDatas = !temPedido;

        labelDataInicial.setEnabled(habilitarDatas);
        setSpinnerHabilitado(spinnerInicio, habilitarDatas);
        labelDataFinal.setEnabled(habilitarDatas);
        setSpinnerHabilitado(spinnerFim, habilitarDatas);

        if (temPedido) {
            labelStatus.setText("Modo de busca por pedido ativado (datas desabilitadas). Pressione Enter ou Pesquisar.");
        } else {
            labelStatus.setText("Escolha o período ou digite o número do pedido e clique em Pesquisar.");
        }
    }

    /**
     * Habilita/desabilita o JSpinner e seu campo de texto interno.
     */
    private static void setSpinnerHabilitado(JSpinner spinner, boolean habilitado) {
        spinner.setEnabled(habilitado);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            JTextField tf = defaultEditor.getTextField();
            tf.setEnabled(habilitado);
            tf.setEditable(habilitado);
        }
    }

    /**
     * Limpa o campo de pedido e reativa os campos de data.
     */
    private void limparFiltros() {
        campoPedido.setText("");
        spinnerInicio.setValue(new java.util.Date());
        spinnerFim.setValue(new java.util.Date());
        modeloTabela.setRowCount(0);
        labelStatus.setText("Filtros limpos. Escolha o período ou digite o número do pedido.");
        campoPedido.requestFocusInWindow();
    }

    /**
     * Cria um campo de seleção de data (com setinhas para subir/descer o dia).
     * Começa com a data de hoje e mostra no formato dd/MM/yyyy.
     */
    private static JSpinner criarSeletorDeData() {
        SpinnerDateModel modelo = new SpinnerDateModel(
                new java.util.Date(), null, null, Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(modelo);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
        return spinner;
    }

    /**
     * Converte o valor do JSpinner (java.util.Date) em LocalDate (só a data, sem hora),
     * garantindo o commit de qualquer valor digitado manualmente no campo de texto.
     */
    private static LocalDate lerData(JSpinner spinner) throws java.text.ParseException {
        spinner.commitEdit();
        java.util.Date data = (java.util.Date) spinner.getValue();
        return data.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Valida os filtros escolhidos e inicia a consulta em segundo plano. */
    private void pesquisar() {
        String textoPedido = campoPedido.getText().trim();
        final Integer filtroPedido;
        final LocalDate inicio;
        final LocalDate fim;

        if (!textoPedido.isEmpty()) {
            // ----- BUSCA POR NÚMERO DO PEDIDO -----
            try {
                int num = Integer.parseInt(textoPedido);
                if (num <= 0) {
                    JOptionPane.showMessageDialog(this,
                            "O número do pedido deve ser maior que zero.",
                            "Número inválido", JOptionPane.WARNING_MESSAGE);
                    campoPedido.requestFocusInWindow();
                    return;
                }
                filtroPedido = num;
                inicio = null;
                fim = null;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Por favor, digite apenas números no campo Nº Pedido.",
                        "Número de pedido inválido", JOptionPane.WARNING_MESSAGE);
                campoPedido.requestFocusInWindow();
                return;
            }
        } else {
            // ----- BUSCA POR PERÍODO DE DATAS -----
            filtroPedido = null;
            LocalDate dataIni;
            LocalDate dataFim;

            try {
                dataIni = lerData(spinnerInicio);
            } catch (java.text.ParseException e) {
                JOptionPane.showMessageDialog(this,
                        "Data inicial inválida. Digite no formato dd/MM/yyyy.",
                        "Data inválida", JOptionPane.WARNING_MESSAGE);
                spinnerInicio.requestFocusInWindow();
                return;
            }

            try {
                dataFim = lerData(spinnerFim);
            } catch (java.text.ParseException e) {
                JOptionPane.showMessageDialog(this,
                        "Data final inválida. Digite no formato dd/MM/yyyy.",
                        "Data inválida", JOptionPane.WARNING_MESSAGE);
                spinnerFim.requestFocusInWindow();
                return;
            }

            // 1 - Validação: data final não pode ser menor que data inicial
            if (dataFim.isBefore(dataIni)) {
                JOptionPane.showMessageDialog(this,
                        "A data final não pode ser menor que a data inicial.",
                        "Período inválido", JOptionPane.WARNING_MESSAGE);
                spinnerFim.requestFocusInWindow();
                return;
            }

            inicio = dataIni;
            fim = dataFim;
        }

        // Desabilita os botões e avisa que está buscando
        botaoPesquisar.setEnabled(false);
        botaoLimpar.setEnabled(false);
        botaoImprimir.setEnabled(false);
        labelStatus.setText("Pesquisando...");
        modeloTabela.setRowCount(0); // limpa resultados anteriores

        // SwingWorker executa a consulta numa outra thread
        new SwingWorker<List<Object[]>, Void>() {

            @Override
            protected List<Object[]> doInBackground() throws Exception {
                if (filtroPedido != null) {
                    return buscarPorPedido(filtroPedido);
                } else {
                    return buscarPedidosPorPeriodo(inicio, fim);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> linhas = get();
                    for (Object[] linha : linhas) {
                        modeloTabela.addRow(linha);
                    }
                    if (linhas.isEmpty()) {
                        if (filtroPedido != null) {
                            labelStatus.setText("Nenhum registro encontrado para o pedido nº " + filtroPedido + ".");
                        } else {
                            labelStatus.setText("Nenhum pedido liberado no período informado.");
                        }
                    } else {
                        if (filtroPedido != null) {
                            labelStatus.setText(linhas.size() + " registro(s) encontrado(s) para o pedido nº " + filtroPedido + ".");
                        } else {
                            labelStatus.setText(linhas.size() + " pedido(s) liberado(s) no período.");
                        }
                    }
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    labelStatus.setText("Erro ao consultar o banco.");
                    JOptionPane.showMessageDialog(PedidosLiberadosApp.this,
                            "Erro: " + causa.getMessage(),
                            "Erro de banco de dados", JOptionPane.ERROR_MESSAGE);
                } finally {
                    botaoPesquisar.setEnabled(true);
                    botaoLimpar.setEnabled(true);
                    botaoImprimir.setEnabled(true);
                }
            }
        }.execute();
    }

    /**
     * Conecta no DB2, busca os pedidos liberados dentro do período informado.
     */
    private List<Object[]> buscarPedidosPorPeriodo(LocalDate inicio, LocalDate fim) throws Exception {
        validarConfiguracoes();
        List<Object[]> resultado = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(SQL_POR_DATA)) {

            ps.setDate(1, Date.valueOf(inicio));
            ps.setDate(2, Date.valueOf(fim.plusDays(1)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object nroPedido = rs.getObject("NRO_PEDIDO");
                    Timestamp dtLiberacao = rs.getTimestamp("DT_LIBERACAO");

                    String dataTexto = (dtLiberacao == null)
                            ? ""
                            : dtLiberacao.toLocalDateTime().format(FORMATO_TELA);

                    resultado.add(new Object[]{nroPedido, dataTexto});
                }
            }
        }
        return resultado;
    }

    /**
     * Conecta no DB2, busca o pedido pelo seu número exato.
     */
    private List<Object[]> buscarPorPedido(int nroPedido) throws Exception {
        validarConfiguracoes();
        List<Object[]> resultado = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(SQL_POR_PEDIDO)) {

            ps.setInt(1, nroPedido);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object nro = rs.getObject("NRO_PEDIDO");
                    Timestamp dtLiberacao = rs.getTimestamp("DT_LIBERACAO");

                    String dataTexto = (dtLiberacao == null)
                            ? "Não liberado"
                            : dtLiberacao.toLocalDateTime().format(FORMATO_TELA);

                    resultado.add(new Object[]{nro, dataTexto});
                }
            }
        }
        return resultado;
    }

    /**
     * Abre a janela de visualização de impressão no modo Paisagem (Deitado)
     * com fontes ampliadas e opções de impressão direta ou exportação/visualização como PDF no navegador.
     */
    private void imprimir() {
        if (modeloTabela.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Não há nenhum registro pesquisado na tabela para imprimir.",
                    "Tabela vazia", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            String subtitulo;
            String textoPedido = campoPedido.getText().trim();

            if (!textoPedido.isEmpty()) {
                subtitulo = "Relatório do Pedido Liberado Nº " + textoPedido;
            } else {
                LocalDate dataIni = lerData(spinnerInicio);
                LocalDate dataFim = lerData(spinnerFim);
                subtitulo = "Relatório de Pedidos Liberados — Período: " +
                        dataIni.format(FORMATO_TELA) + " a " + dataFim.format(FORMATO_TELA);
            }

            // Coleta os registros exibidos na tabela
            List<Object[]> linhas = new ArrayList<>();
            for (int r = 0; r < modeloTabela.getRowCount(); r++) {
                Object nro = modeloTabela.getValueAt(r, 0);
                Object dt  = modeloTabela.getValueAt(r, 1);
                linhas.add(new Object[]{nro, dt});
            }

            RelatorioImpressao relatorio = new RelatorioImpressao(subtitulo, linhas);
            VisualizadorImpressaoDialog visualizador = new VisualizadorImpressaoDialog(this, relatorio);
            visualizador.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao gerar relatório de impressão: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Ponto de entrada do programa: abre a janela. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PedidosLiberadosApp().setVisible(true));
    }
}
