package br.com.bartz.pedidos;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.io.File;

/**
 * Janela de visualização prévia da impressão (Print Preview) em modo Paisagem (Deitado).
 * Mostra exatamente como o relatório sairá no papel, com botões para imprimir
 * na impressora física ou abrir no navegador para salvar em PDF.
 */
public class VisualizadorImpressaoDialog extends JDialog {

    private final RelatorioImpressao relatorio;
    private int paginaAtual = 0;

    private final JLabel labelPagina = new JLabel();
    private final JButton botaoAnterior = new JButton("◀ Anterior");
    private final JButton botaoProxima = new JButton("Próxima ▶");
    private final JPanel painelFolha;

    public VisualizadorImpressaoDialog(Frame parent, RelatorioImpressao relatorio) {
        super(parent, "Visualização de Impressão - Modo Paisagem (Deitado)", true);
        this.relatorio = relatorio;

        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // ===== BARRA DE FERRAMENTAS SUPERIOR =====
        JPanel barraSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        barraSuperior.setBackground(new Color(245, 247, 250));
        barraSuperior.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 215, 220)));

        // Navegação de páginas (Anterior, indicador e Próxima lado a lado)
        botaoAnterior.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botaoAnterior.addActionListener(e -> mudarPagina(-1));

        labelPagina.setFont(new Font("Segoe UI", Font.BOLD, 13));
        labelPagina.setForeground(new Color(30, 30, 30));

        botaoProxima.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botaoProxima.addActionListener(e -> mudarPagina(1));

        barraSuperior.add(botaoAnterior);
        barraSuperior.add(labelPagina);
        barraSuperior.add(botaoProxima);

        // Separador vertical
        JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
        sep1.setPreferredSize(new Dimension(2, 26));
        barraSuperior.add(sep1);

        // Botões de ação
        JButton botaoImprimir = new JButton("Imprimir na Impressora");
        botaoImprimir.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botaoImprimir.setBackground(new Color(31, 78, 121));
        botaoImprimir.setForeground(Color.WHITE);
        botaoImprimir.addActionListener(e -> imprimirDireto());

        JButton botaoNavegador = new JButton("Abrir no Navegador (PDF)");
        botaoNavegador.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botaoNavegador.addActionListener(e -> abrirNoNavegador());

        barraSuperior.add(botaoImprimir);
        barraSuperior.add(botaoNavegador);

        // Separador vertical
        JSeparator sep2 = new JSeparator(SwingConstants.VERTICAL);
        sep2.setPreferredSize(new Dimension(2, 26));
        barraSuperior.add(sep2);

        JButton botaoFechar = new JButton("Fechar");
        botaoFechar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        botaoFechar.addActionListener(e -> dispose());
        barraSuperior.add(botaoFechar);

        add(barraSuperior, BorderLayout.NORTH);

        // Atalhos de teclado para navegação fácil
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke("LEFT"), "ant");
        im.put(KeyStroke.getKeyStroke("PAGE_UP"), "ant");
        am.put("ant", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { mudarPagina(-1); }
        });

        im.put(KeyStroke.getKeyStroke("RIGHT"), "prox");
        im.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "prox");
        am.put("prox", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { mudarPagina(1); }
        });

        im.put(KeyStroke.getKeyStroke("ESCAPE"), "fechar");
        am.put("fechar", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }
        });

        // ===== PAINEL CENTRAL: FOLHA A4 PAISAGEM =====
        painelFolha = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();

                // Dimensões A4 Paisagem (842 x 595 pontos)
                int folhaW = RelatorioImpressao.LARGURA_A4_PAISAGEM;
                int folhaH = RelatorioImpressao.ALTURA_A4_PAISAGEM;

                // Centraliza a folha no painel
                int folhaX = Math.max(20, (getWidth() - folhaW) / 2);
                int folhaY = 20;

                // Sombra suave (drop shadow)
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRoundRect(folhaX + 4, folhaY + 4, folhaW, folhaH, 8, 8);

                // Folha branca
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(folhaX, folhaY, folhaW, folhaH, 6, 6);

                g2.setColor(new Color(190, 195, 200));
                g2.drawRoundRect(folhaX, folhaY, folhaW, folhaH, 6, 6);

                // Desenha o conteúdo da página atual
                Graphics2D gConteudo = (Graphics2D) g2.create(folhaX, folhaY, folhaW, folhaH);
                relatorio.desenharPagina(gConteudo, paginaAtual, folhaW, folhaH);
                gConteudo.dispose();

                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(RelatorioImpressao.LARGURA_A4_PAISAGEM + 60, RelatorioImpressao.ALTURA_A4_PAISAGEM + 50);
            }
        };
        painelFolha.setBackground(new Color(82, 86, 89)); // cinza escuro estilo leitor de PDF

        JScrollPane scroll = new JScrollPane(painelFolha);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        atualizarNavegacao();

        setSize(1060, 750);
        setLocationRelativeTo(parent);
    }

    private void mudarPagina(int delta) {
        int nova = paginaAtual + delta;
        if (nova >= 0 && nova < relatorio.getTotalPaginas()) {
            paginaAtual = nova;
            atualizarNavegacao();
            painelFolha.repaint();
        }
    }

    private void atualizarNavegacao() {
        labelPagina.setText("Página " + (paginaAtual + 1) + " de " + relatorio.getTotalPaginas());
        botaoAnterior.setEnabled(paginaAtual > 0);
        botaoProxima.setEnabled(paginaAtual < relatorio.getTotalPaginas() - 1);
    }

    /**
     * Envia o relatório para a impressora física com configuração de Paisagem e fonte grande.
     */
    private void imprimirDireto() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Relatório de Pedidos Liberados - Bartz");

            PageFormat pf = job.defaultPage();
            Paper paper = new Paper();
            paper.setSize(595.27, 841.89);
            paper.setImageableArea(28, 28, 595.27 - 56, 841.89 - 56);
            pf.setPaper(paper);
            pf.setOrientation(PageFormat.LANDSCAPE);

            job.setPrintable(relatorio, pf);

            if (job.printDialog()) {
                job.print();
                JOptionPane.showMessageDialog(this,
                        "Relatório enviado para a impressora com sucesso!",
                        "Impressão", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao imprimir: " + ex.getMessage(),
                    "Erro de Impressão", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Exporta o relatório como HTML e abre imediatamente no navegador padrão (Edge/Chrome),
     * permitindo que o usuário visualize em tela cheia e use o "Salvar como PDF" ou imprima pelo navegador.
     */
    private void abrirNoNavegador() {
        try {
            File tempHtml = new File(System.getProperty("java.io.tmpdir"), "relatorio-pedidos-liberados.html");
            relatorio.exportarHtml(tempHtml);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(tempHtml.toURI());
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(tempHtml);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Arquivo gerado em:\n" + tempHtml.getAbsolutePath(),
                        "Relatório HTML/PDF Gerado", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Não foi possível abrir o navegador: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}

