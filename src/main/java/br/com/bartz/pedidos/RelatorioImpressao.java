package br.com.bartz.pedidos;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Responsável pela renderização do relatório no modo Paisagem (Deitado)
 * com fonte grande e legível, tanto para a impressora física quanto para
 * o visualizador de tela e exportação para HTML/PDF no navegador.
 */
public class RelatorioImpressao implements Printable {

    public static final int LARGURA_A4_PAISAGEM = 842; // pontos (72 DPI)
    public static final int ALTURA_A4_PAISAGEM  = 595; // pontos (72 DPI)
    public static final int LINHAS_POR_PAGINA   = 16;

    private final String subtitulo;
    private final List<Object[]> linhas;
    private final String dataEmissao;
    private final int totalPaginas;

    public RelatorioImpressao(String subtitulo, List<Object[]> linhas) {
        this.subtitulo = subtitulo;
        this.linhas = linhas;
        this.dataEmissao = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        int total = linhas.size();
        this.totalPaginas = Math.max(1, (int) Math.ceil((double) total / LINHAS_POR_PAGINA));
    }

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public int getTotalRegistros() {
        return linhas.size();
    }

    /**
     * Implementação de java.awt.print.Printable para envio direto à impressora.
     */
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex >= totalPaginas) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        desenharPagina(g2d, pageIndex, (int) pageFormat.getImageableWidth(), (int) pageFormat.getImageableHeight());
        return PAGE_EXISTS;
    }

    /**
     * Desenha uma página do relatório com fonte grande, cabeçalhos destacados e modo deitado.
     */
    public void desenharPagina(Graphics2D g2, int pageIndex, int larguraTotal, int alturaTotal) {
        // Ativa suavização de fontes e formas geométricas
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int margemX = 40;
        int margemY = 30;
        int larguraUtil = larguraTotal - (margemX * 2);
        int y = margemY;

        // ----- 1. CABEÇALHO DA EMPRESA E RELATÓRIO -----
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2.setColor(new Color(31, 78, 121)); // azul escuro corporativo
        g2.drawString("BARTZ MÓVEIS PLANEJADOS", margemX, y + 16);

        // Data/Hora de emissão no canto direito
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(100, 100, 100));
        String textoEmissao = "Emitido em: " + dataEmissao;
        int emissaoWidth = g2.getFontMetrics().stringWidth(textoEmissao);
        g2.drawString(textoEmissao, margemX + larguraUtil - emissaoWidth, y + 16);

        y += 24;

        // Subtítulo do filtro
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(new Color(60, 64, 67));
        g2.drawString(subtitulo, margemX, y + 14);

        y += 22;

        // Linha divisória horizontal elegante
        g2.setColor(new Color(31, 78, 121));
        g2.fillRect(margemX, y, larguraUtil, 2);

        y += 12;

        // ----- 2. CABEÇALHO DA TABELA -----
        int alturaCabecalhoTab = 28;
        int colWidth = larguraUtil / 2;

        g2.setColor(new Color(238, 242, 246));
        g2.fillRect(margemX, y, larguraUtil, alturaCabecalhoTab);

        g2.setColor(new Color(185, 195, 205));
        g2.drawRect(margemX, y, larguraUtil, alturaCabecalhoTab);
        g2.drawLine(margemX + colWidth, y, margemX + colWidth, y + alturaCabecalhoTab);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(new Color(33, 37, 41));

        desenharTextoCentralizado(g2, "Nº DO PEDIDO", margemX, y, colWidth, alturaCabecalhoTab);
        desenharTextoCentralizado(g2, "DATA DE LIBERAÇÃO", margemX + colWidth, y, colWidth, alturaCabecalhoTab);

        y += alturaCabecalhoTab;

        // ----- 3. LINHAS DE DADOS (FONTE GRANDE E LEGÍVEL) -----
        int alturaLinha = 25;
        int inicioLinha = pageIndex * LINHAS_POR_PAGINA;
        int fimLinha = Math.min(inicioLinha + LINHAS_POR_PAGINA, linhas.size());

        Font fontePedido = new Font("Segoe UI", Font.BOLD, 14);
        Font fonteData   = new Font("Segoe UI", Font.PLAIN, 14);

        for (int i = inicioLinha; i < fimLinha; i++) {
            Object[] item = linhas.get(i);
            String nroPedido = item[0] != null ? item[0].toString() : "";
            String dataLib   = item[1] != null ? item[1].toString() : "";

            // Fundo zebrado para facilitar a leitura
            if (i % 2 == 1) {
                g2.setColor(new Color(248, 250, 252));
                g2.fillRect(margemX, y, larguraUtil, alturaLinha);
            }

            // Bordas das células
            g2.setColor(new Color(225, 230, 235));
            g2.drawRect(margemX, y, larguraUtil, alturaLinha);
            g2.drawLine(margemX + colWidth, y, margemX + colWidth, y + alturaLinha);

            // Texto do pedido (14pt Bold centralizado)
            g2.setFont(fontePedido);
            g2.setColor(new Color(20, 20, 20));
            desenharTextoCentralizado(g2, nroPedido, margemX, y, colWidth, alturaLinha);

            // Texto da data (14pt Plain centralizado)
            g2.setFont(fonteData);
            g2.setColor(new Color(40, 40, 40));
            desenharTextoCentralizado(g2, dataLib, margemX + colWidth, y, colWidth, alturaLinha);

            y += alturaLinha;
        }

        // ----- 4. RODAPÉ DA PÁGINA -----
        int rodapeY = alturaTotal - margemY - 18;

        g2.setColor(new Color(210, 215, 220));
        g2.drawLine(margemX, rodapeY, margemX + larguraUtil, rodapeY);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(new Color(100, 100, 100));
        g2.drawString("Total de pedidos liberados: " + linhas.size(), margemX, rodapeY + 16);

        String textoPag = "Página " + (pageIndex + 1) + " de " + totalPaginas;
        int pagWidth = g2.getFontMetrics().stringWidth(textoPag);
        g2.drawString(textoPag, margemX + larguraUtil - pagWidth, rodapeY + 16);
    }

    /**
     * Auxiliar para desenhar texto centralizado horizontal e verticalmente dentro de uma célula.
     */
    private static void desenharTextoCentralizado(Graphics2D g2, String texto, int x, int y, int largura, int altura) {
        if (texto == null || texto.isEmpty()) return;
        FontMetrics fm = g2.getFontMetrics();
        int textoX = x + (largura - fm.stringWidth(texto)) / 2;
        int textoY = y + (altura - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(texto, textoX, textoY);
    }

    /**
     * Gera um arquivo HTML formatado para impressão em modo Paisagem (deitado)
     * e abre diretamente no navegador do usuário (Edge/Chrome) para visualização ou salvar como PDF.
     */
    public void exportarHtml(File destino) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(destino, StandardCharsets.UTF_8))) {
            pw.println("<!DOCTYPE html>");
            pw.println("<html lang=\"pt-BR\">");
            pw.println("<head>");
            pw.println("  <meta charset=\"UTF-8\">");
            pw.println("  <title>" + subtitulo + "</title>");
            pw.println("  <style>");
            pw.println("    @page {");
            pw.println("      size: A4 landscape;");
            pw.println("      margin: 15mm;");
            pw.println("    }");
            pw.println("    @media print {");
            pw.println("      .no-print { display: none !important; }");
            pw.println("      body { background: white; margin: 0; padding: 0; }");
            pw.println("      .folha { box-shadow: none !important; border: none !important; width: 100% !important; margin: 0 !important; }");
            pw.println("    }");
            pw.println("    body {");
            pw.println("      font-family: 'Segoe UI', Arial, sans-serif;");
            pw.println("      background-color: #525659;");
            pw.println("      margin: 0;");
            pw.println("      padding: 20px;");
            pw.println("      color: #222;");
            pw.println("    }");
            pw.println("    .barra-botoes {");
            pw.println("      max-width: 1050px;");
            pw.println("      margin: 0 auto 15px auto;");
            pw.println("      display: flex;");
            pw.println("      justify-content: space-between;");
            pw.println("      align-items: center;");
            pw.println("      background: #333;");
            pw.println("      padding: 12px 20px;");
            pw.println("      border-radius: 6px;");
            pw.println("      color: white;");
            pw.println("    }");
            pw.println("    .btn {");
            pw.println("      background-color: #0078d4;");
            pw.println("      color: white;");
            pw.println("      border: none;");
            pw.println("      padding: 10px 20px;");
            pw.println("      font-size: 15px;");
            pw.println("      font-weight: bold;");
            pw.println("      border-radius: 4px;");
            pw.println("      cursor: pointer;");
            pw.println("    }");
            pw.println("    .btn:hover { background-color: #005a9e; }");
            pw.println("    .folha {");
            pw.println("      max-width: 1050px;");
            pw.println("      margin: 0 auto;");
            pw.println("      background: white;");
            pw.println("      padding: 35px 40px;");
            pw.println("      box-shadow: 0 4px 15px rgba(0,0,0,0.3);");
            pw.println("      border-radius: 4px;");
            pw.println("      min-height: 600px;");
            pw.println("    }");
            pw.println("    .cabecalho {");
            pw.println("      border-bottom: 2px solid #1F4E79;");
            pw.println("      padding-bottom: 12px;");
            pw.println("      margin-bottom: 20px;");
            pw.println("      display: flex;");
            pw.println("      justify-content: space-between;");
            pw.println("      align-items: flex-end;");
            pw.println("    }");
            pw.println("    .empresa { font-size: 24px; font-weight: bold; color: #1F4E79; }");
            pw.println("    .subtitulo { font-size: 16px; font-weight: bold; color: #444; margin-top: 5px; }");
            pw.println("    .emissao { font-size: 13px; color: #777; text-align: right; }");
            pw.println("    table {");
            pw.println("      width: 100%;");
            pw.println("      border-collapse: collapse;");
            pw.println("      margin-top: 15px;");
            pw.println("      font-size: 16px;"); // Fonte grande
            pw.println("    }");
            pw.println("    th {");
            pw.println("      background-color: #EEF2F6;");
            pw.println("      color: #212529;");
            pw.println("      padding: 12px;");
            pw.println("      text-align: center;");
            pw.println("      border: 1px solid #B9C3CD;");
            pw.println("      font-size: 15px;");
            pw.println("    }");
            pw.println("    td {");
            pw.println("      padding: 10px;");
            pw.println("      text-align: center;");
            pw.println("      border: 1px solid #E1E6EB;");
            pw.println("    }");
            pw.println("    tr:nth-child(even) td { background-color: #F8FAFC; }");
            pw.println("    .rodape {");
            pw.println("      margin-top: 30px;");
            pw.println("      padding-top: 12px;");
            pw.println("      border-top: 1px solid #D2D7DC;");
            pw.println("      display: flex;");
            pw.println("      justify-content: space-between;");
            pw.println("      font-size: 13px;");
            pw.println("      color: #666;");
            pw.println("      font-weight: bold;");
            pw.println("    }");
            pw.println("  </style>");
            pw.println("</head>");
            pw.println("<body>");
            pw.println("  <div class=\"no-print barra-botoes\">");
            pw.println("    <span>Modo Paisagem (Deitado) • Letra ampliada</span>");
            pw.println("    <button class=\"btn\" onclick=\"window.print()\">🖨️ Imprimir / Salvar em PDF</button>");
            pw.println("  </div>");
            pw.println("  <div class=\"folha\">");
            pw.println("    <div class=\"cabecalho\">");
            pw.println("      <div>");
            pw.println("        <div class=\"empresa\">BARTZ MÓVEIS PLANEJADOS</div>");
            pw.println("        <div class=\"subtitulo\">" + subtitulo + "</div>");
            pw.println("      </div>");
            pw.println("      <div class=\"emissao\">Emitido em:<br><b>" + dataEmissao + "</b></div>");
            pw.println("    </div>");
            pw.println("    <table>");
            pw.println("      <thead>");
            pw.println("        <tr>");
            pw.println("          <th style=\"width: 50%;\">Nº DO PEDIDO</th>");
            pw.println("          <th style=\"width: 50%;\">DATA DE LIBERAÇÃO</th>");
            pw.println("        </tr>");
            pw.println("      </thead>");
            pw.println("      <tbody>");

            for (Object[] item : linhas) {
                String nro = item[0] != null ? item[0].toString() : "";
                String dt  = item[1] != null ? item[1].toString() : "";
                pw.println("        <tr>");
                pw.println("          <td><b>" + nro + "</b></td>");
                pw.println("          <td>" + dt + "</td>");
                pw.println("        </tr>");
            }

            pw.println("      </tbody>");
            pw.println("    </table>");
            pw.println("    <div class=\"rodape\">");
            pw.println("      <div>Total de pedidos liberados: " + linhas.size() + "</div>");
            pw.println("      <div>Bartz Móveis Planejados</div>");
            pw.println("    </div>");
            pw.println("  </div>");
            pw.println("</body>");
            pw.println("</html>");
        }
    }
}

