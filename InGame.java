package Vista;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * InGame rediseñado:
 *  - Historial de rondas reubicado (no tapa el panel de cantos)
 *  - Zona de cartas tiradas más visible en la mesa
 *  - Panel de cantos más arriba y separado del historial
 */
public class InGame extends JFrame {

    public  static final int CARD_W_PUBLIC = 90;
    public  static final int CARD_H_PUBLIC = 140;
    private static final int CARD_W = CARD_W_PUBLIC;
    private static final int CARD_H = CARD_H_PUBLIC;

    private static final Color FELT_DARK   = new Color(27, 94, 32);
    private static final Color FELT_MED    = new Color(46, 125, 50);
    private static final Color GOLD        = new Color(212, 175, 55);
    private static final Color GOLD_DARK   = new Color(153, 120, 20);
    private static final Color CARD_BACK   = new Color(180, 0, 0);
    private static final Color BTN_ENVIDO  = new Color(20, 60, 150);
    private static final Color BTN_TRUCO   = new Color(140, 20, 20);
    private static final Color BTN_ACCEPT  = new Color(30, 120, 30);
    private static final Color BTN_REJECT  = new Color(140, 20, 20);
    private static final Color TEXT_LIGHT  = new Color(255, 245, 200);

    // Mano jugador
    public JLabel  LabelCarta1, LabelCarta2, LabelCarta3;
    public JButton BotonCarta1, BotonCarta2, BotonCarta3;

    // Mano IA (boca abajo)
    public JLabel LabelCartaIA1, LabelCartaIA2, LabelCartaIA3;

    // Cartas en mesa (ronda actual)
    public JLabel LabelCartaJugador;
    public JLabel LabelCartaIA;

    // Historial de rondas — REUBICADO debajo de las cartas en mesa,
    // centrado, sin tapar el panel de cantos
    public JLabel[] SlotRondaJugador = new JLabel[3];
    public JLabel[] SlotRondaIA      = new JLabel[3];

    // Puntajes
    public JLabel PuntajeJugador, PuntajeIA;

    // Mensaje
    public JLabel LabelMensaje;

    // Botones de canto
    public JButton BtnEnvido, BtnRealEnvido, BtnFaltaEnvido;
    public JButton BtnTruco, BtnRetruco, BtnVale4;
    public JButton BtnQuiero, BtnNoQuiero;

    // Panel Perceptrones
    public JPanel  PanelPerceptrones;
    public JLabel  LblEnvidoW0, LblEnvidoW1, LblEnvidoW2, LblEnvidoX1,
                   LblEnvidoNet, LblEnvidoSalida, LblEnvidoError;
    public JLabel  LblTrucoW0, LblTrucoW1, LblTrucoW2, LblTrucoX1,
                   LblTrucoNet, LblTrucoSalida, LblTrucoError;
    public JLabel  LblEstadoW0, LblEstadoW1, LblEstadoW2, LblEstadoX1,
                   LblEstadoNet, LblEstadoSalida, LblEstadoError;
    public JLabel  LblDecision;

    public InGame() {
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Truco Argentino – IA Perceptrónica");
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(FELT_DARK);
        setContentPane(root);

        JPanel mesa = buildMesa();
        root.add(mesa, BorderLayout.CENTER);

        buildPanelPerceptrones();
        JScrollPane scrollP = new JScrollPane(PanelPerceptrones,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollP.setPreferredSize(new Dimension(270, 820));
        scrollP.setBorder(BorderFactory.createLineBorder(GOLD_DARK, 1));
        root.add(scrollP, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildMesa() {
        JPanel mesa = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                int w = getWidth(), h = getHeight();
                g2.setColor(FELT_DARK);
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(0, 0, 0, 18));
                for (int i = -h; i < w + h; i += 12) g2.drawLine(i, 0, i + h, h);
                for (int i = -h; i < w + h; i += 12) g2.drawLine(i + h, 0, i, h);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(w / 2f, h / 4f, FELT_MED, w / 2f, 3 * h / 4f, FELT_DARK);
                g2.setPaint(gp);
                g2.fillOval(w / 2 - 300, h / 2 - 220, 600, 440);
                g2.setColor(GOLD_DARK);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRect(10, 10, w - 20, h - 20);
                g2.setColor(new Color(212, 175, 55, 80));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRect(16, 16, w - 32, h - 32);
            }
        };
        mesa.setLayout(null);
        mesa.setPreferredSize(new Dimension(800, 820));

        // ── PUNTAJES ──────────────────────────────────────────────────────────
        PuntajeIA = makeScoreLabel("IA: 0");
        PuntajeIA.setBounds(320, 12, 160, 28);
        mesa.add(PuntajeIA);

        PuntajeJugador = makeScoreLabel("Vos: 0");
        PuntajeJugador.setBounds(320, 782, 160, 28);
        mesa.add(PuntajeJugador);

        // ── MANO IA (arriba, boca abajo) ─────────────────────────────────────
        int iaY = 48;
        LabelCartaIA1 = makeCardBackLabel();
        LabelCartaIA1.setBounds(220, iaY, CARD_W, CARD_H);
        mesa.add(LabelCartaIA1);

        LabelCartaIA2 = makeCardBackLabel();
        LabelCartaIA2.setBounds(335, iaY, CARD_W, CARD_H);
        mesa.add(LabelCartaIA2);

        LabelCartaIA3 = makeCardBackLabel();
        LabelCartaIA3.setBounds(450, iaY, CARD_W, CARD_H);
        mesa.add(LabelCartaIA3);

        JLabel lblIA = new JLabel("IA", SwingConstants.CENTER);
        lblIA.setFont(new Font("Georgia", Font.BOLD | Font.ITALIC, 13));
        lblIA.setForeground(new Color(200, 200, 200));
        lblIA.setBounds(335, 32, 90, 16);
        mesa.add(lblIA);

        // ── ZONA CENTRAL — cartas tiradas ─────────────────────────────────────
        // iaY(48) + CARD_H(140) = 188 + margen 18 = 206
        int mesaY = 206;

        JLabel lblTiradaJugador = makeSmallLabel("Jugaste:");
        lblTiradaJugador.setBounds(225, mesaY - 16, 100, 16);
        mesa.add(lblTiradaJugador);

        LabelCartaJugador = makeEmptyCardSlot("—");
        LabelCartaJugador.setBounds(225, mesaY, CARD_W, CARD_H);
        mesa.add(LabelCartaJugador);

        JLabel lblTiradaIA = makeSmallLabel("IA tiró:");
        lblTiradaIA.setBounds(460, mesaY - 16, 100, 16);
        mesa.add(lblTiradaIA);

        LabelCartaIA = makeEmptyCardSlot("—");
        LabelCartaIA.setBounds(460, mesaY, CARD_W, CARD_H);
        mesa.add(LabelCartaIA);

        // ── MENSAJE CENTRAL ───────────────────────────────────────────────────
        // mesaY(206) + CARD_H(140) = 346 + 8 = 354
        LabelMensaje = new JLabel("Elegí una carta o cantá", SwingConstants.CENTER);
        LabelMensaje.setFont(new Font("Georgia", Font.BOLD | Font.ITALIC, 13));
        LabelMensaje.setForeground(TEXT_LIGHT);
        LabelMensaje.setBorder(new EmptyBorder(4, 8, 4, 8));
        LabelMensaje.setBounds(155, 354, 480, 48);
        mesa.add(LabelMensaje);

        // ── HISTORIAL DE RONDAS ── 354+48=402, historial en 412
        int miniW = 52, miniH = 80;
        int histBaseY = 412;
        int centroX   = 400;

        JLabel lblHistJ = makeSmallLabel("Rondas vos:");
        lblHistJ.setBounds(centroX - 3 * (miniW + 5) - 10, histBaseY - 16, 140, 14);
        mesa.add(lblHistJ);

        for (int i = 0; i < 3; i++) {
            SlotRondaJugador[i] = makeEmptyCardSlot("");
            int x = centroX - (3 - i) * (miniW + 5) - 5;
            SlotRondaJugador[i].setBounds(x, histBaseY, miniW, miniH);
            SlotRondaJugador[i].setFont(SlotRondaJugador[i].getFont().deriveFont(9f));
            SlotRondaJugador[i].setVisible(false);
            mesa.add(SlotRondaJugador[i]);
        }

        JLabel lblHistIA = makeSmallLabel("Rondas IA:");
        lblHistIA.setBounds(centroX + 10, histBaseY - 16, 140, 14);
        mesa.add(lblHistIA);

        for (int i = 0; i < 3; i++) {
            SlotRondaIA[i] = makeEmptyCardSlot("");
            int x = centroX + 10 + i * (miniW + 5);
            SlotRondaIA[i].setBounds(x, histBaseY, miniW, miniH);
            SlotRondaIA[i].setFont(SlotRondaIA[i].getFont().deriveFont(9f));
            SlotRondaIA[i].setVisible(false);
            mesa.add(SlotRondaIA[i]);
        }

        // ── MANO DEL JUGADOR — boca arriba, abajo ────────────────────────────
        // histBaseY(412) + miniH(80) = 492 + 18 margen = 510
        int playerY = 514;
        LabelCarta1 = makeCardLabel();
        LabelCarta1.setBounds(220, playerY, CARD_W, CARD_H);
        mesa.add(LabelCarta1);

        LabelCarta2 = makeCardLabel();
        LabelCarta2.setBounds(335, playerY, CARD_W, CARD_H);
        mesa.add(LabelCarta2);

        LabelCarta3 = makeCardLabel();
        LabelCarta3.setBounds(450, playerY, CARD_W, CARD_H);
        mesa.add(LabelCarta3);

        // playerY(514) + CARD_H(140) = 654 + 6 = 660
        BotonCarta1 = makeCardButton("Jugar");
        BotonCarta1.setBounds(220, playerY + CARD_H + 6, CARD_W, 28);
        mesa.add(BotonCarta1);

        BotonCarta2 = makeCardButton("Jugar");
        BotonCarta2.setBounds(335, playerY + CARD_H + 6, CARD_W, 28);
        mesa.add(BotonCarta2);

        BotonCarta3 = makeCardButton("Jugar");
        BotonCarta3.setBounds(450, playerY + CARD_H + 6, CARD_W, 28);
        mesa.add(BotonCarta3);

        JLabel lblVos = new JLabel("VOS", SwingConstants.CENTER);
        lblVos.setFont(new Font("Georgia", Font.BOLD | Font.ITALIC, 13));
        lblVos.setForeground(GOLD);
        lblVos.setBounds(335, playerY - 18, 90, 16);
        mesa.add(lblVos);

        // ── PANEL DE CANTOS — lado izquierdo ──────────────────────────────────
        JPanel panelCantos = buildPanelCantos();
        panelCantos.setBounds(16, 200, 148, 316);
        mesa.add(panelCantos);

        // ── PANEL RESPUESTA (¿Querés?) — lado derecho ─────────────────────────
        JPanel panelRespuesta = buildPanelRespuesta();
        panelRespuesta.setBounds(638, 200, 132, 100);
        mesa.add(panelRespuesta);

        return mesa;
    }

    private JPanel buildPanelCantos() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(0, 0, 0, 110));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(GOLD_DARK);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
        };
        p.setLayout(new GridLayout(7, 1, 4, 5));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(8, 6, 8, 6));

        JLabel title = new JLabel("⚑ CANTAR", SwingConstants.CENTER);
        title.setFont(new Font("Georgia", Font.BOLD, 12));
        title.setForeground(GOLD);
        p.add(title);

        BtnEnvido      = makeCantarBtn("Envido",       BTN_ENVIDO);
        BtnRealEnvido  = makeCantarBtn("Real Envido",  BTN_ENVIDO.darker());
        BtnFaltaEnvido = makeCantarBtn("Falta Envido", new Color(10, 40, 120));
        BtnTruco       = makeCantarBtn("Truco",        BTN_TRUCO);
        BtnRetruco     = makeCantarBtn("Retruco",      BTN_TRUCO.darker());
        BtnVale4       = makeCantarBtn("Vale 4",       new Color(100, 10, 10));

        p.add(BtnEnvido);
        p.add(BtnRealEnvido);
        p.add(BtnFaltaEnvido);
        p.add(BtnTruco);
        p.add(BtnRetruco);
        p.add(BtnVale4);

        return p;
    }

    private JPanel buildPanelRespuesta() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(0, 0, 0, 110));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(GOLD_DARK);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
        };
        p.setLayout(new GridLayout(3, 1, 4, 6));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(6, 6, 6, 6));

        JLabel lbl = new JLabel("¿Querés?", SwingConstants.CENTER);
        lbl.setFont(new Font("Georgia", Font.BOLD, 11));
        lbl.setForeground(GOLD);
        p.add(lbl);

        BtnQuiero   = makeCantarBtn("¡Quiero!", BTN_ACCEPT);
        BtnNoQuiero = makeCantarBtn("No quiero", BTN_REJECT);
        BtnQuiero.setVisible(false);
        BtnNoQuiero.setVisible(false);

        p.add(BtnQuiero);
        p.add(BtnNoQuiero);
        return p;
    }

    private void buildPanelPerceptrones() {
        PanelPerceptrones = new JPanel();
        PanelPerceptrones.setBackground(new Color(15, 15, 40));
        PanelPerceptrones.setLayout(new BoxLayout(PanelPerceptrones, BoxLayout.Y_AXIS));
        PanelPerceptrones.setBorder(new EmptyBorder(10, 10, 10, 10));

        addPLabel("═══ PERCEPTRONES IA ═══", new Font("Monospaced", Font.BOLD, 13), Color.CYAN);
        PanelPerceptrones.add(Box.createVerticalStrut(6));

        addPLabel("▶ Perceptrón A – Envido", new Font("Monospaced", Font.BOLD, 11), new Color(255, 200, 0));
        LblEnvidoW0     = addPLabel("  w0: ...", null, Color.WHITE);
        LblEnvidoW1     = addPLabel("  w1: ...", null, Color.WHITE);
        LblEnvidoW2     = addPLabel("  w2: ...", null, Color.WHITE);
        LblEnvidoX1     = addPLabel("  x1: ...", null, Color.LIGHT_GRAY);
        LblEnvidoNet    = addPLabel("  net: ...", null, Color.LIGHT_GRAY);
        LblEnvidoSalida = addPLabel("  SALIDA: ...", new Font("Monospaced", Font.BOLD, 11), Color.GREEN);
        LblEnvidoError  = addPLabel("  error: ...", null, new Color(255, 100, 100));
        PanelPerceptrones.add(Box.createVerticalStrut(6));

        addPLabel("▶ Perceptrón B – Truco", new Font("Monospaced", Font.BOLD, 11), new Color(255, 200, 0));
        LblTrucoW0     = addPLabel("  w0: ...", null, Color.WHITE);
        LblTrucoW1     = addPLabel("  w1: ...", null, Color.WHITE);
        LblTrucoW2     = addPLabel("  w2: ...", null, Color.WHITE);
        LblTrucoX1     = addPLabel("  x1: ...", null, Color.LIGHT_GRAY);
        LblTrucoNet    = addPLabel("  net: ...", null, Color.LIGHT_GRAY);
        LblTrucoSalida = addPLabel("  SALIDA: ...", new Font("Monospaced", Font.BOLD, 11), Color.GREEN);
        LblTrucoError  = addPLabel("  error: ...", null, new Color(255, 100, 100));
        PanelPerceptrones.add(Box.createVerticalStrut(6));

        addPLabel("▶ Perceptrón C – Estado", new Font("Monospaced", Font.BOLD, 11), new Color(255, 200, 0));
        LblEstadoW0     = addPLabel("  w0: ...", null, Color.WHITE);
        LblEstadoW1     = addPLabel("  w1: ...", null, Color.WHITE);
        LblEstadoW2     = addPLabel("  w2: ...", null, Color.WHITE);
        LblEstadoX1     = addPLabel("  x1: ...", null, Color.LIGHT_GRAY);
        LblEstadoNet    = addPLabel("  net: ...", null, Color.LIGHT_GRAY);
        LblEstadoSalida = addPLabel("  SALIDA: ...", new Font("Monospaced", Font.BOLD, 11), Color.GREEN);
        LblEstadoError  = addPLabel("  error: ...", null, new Color(255, 100, 100));
        PanelPerceptrones.add(Box.createVerticalStrut(8));

        addPLabel("⬡ DECISIÓN FINAL", new Font("Monospaced", Font.BOLD, 12), new Color(0, 220, 220));
        LblDecision = addPLabel("  Jugada: ...", new Font("Monospaced", Font.BOLD, 13), Color.YELLOW);
    }

    private JLabel addPLabel(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font != null ? font : new Font("Monospaced", Font.PLAIN, 10));
        lbl.setForeground(color);
        lbl.setOpaque(false);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        PanelPerceptrones.add(lbl);
        return lbl;
    }

    public void actualizarPanelPerceptrones(Modelo.IA ia) {
        LblEnvidoW0.setText(String.format("  w0: %.4f", ia.perceptronEnvido.getW0()));
        LblEnvidoW1.setText(String.format("  w1: %.4f", ia.perceptronEnvido.getW1()));
        LblEnvidoW2.setText(String.format("  w2: %.4f", ia.perceptronEnvido.getW2()));
        LblEnvidoX1.setText(String.format("  x1(tantos/33): %.4f", ia.perceptronEnvido.getUltimaEntrada1()));
        LblEnvidoNet.setText(String.format("  net: %.4f", ia.perceptronEnvido.getUltimaSalidaNeta()));
        int se = ia.perceptronEnvido.getUltimaSalida();
        LblEnvidoSalida.setText("  SALIDA: " + se + (se == 1 ? "  ✔ buen envido" : "  ✘ flojo"));
        LblEnvidoSalida.setForeground(se == 1 ? Color.GREEN : new Color(255, 100, 100));
        LblEnvidoError.setText(String.format("  error: %.4f", ia.perceptronEnvido.getUltimoError()));

        LblTrucoW0.setText(String.format("  w0: %.4f", ia.perceptronTruco.getW0()));
        LblTrucoW1.setText(String.format("  w1: %.4f", ia.perceptronTruco.getW1()));
        LblTrucoW2.setText(String.format("  w2: %.4f", ia.perceptronTruco.getW2()));
        LblTrucoX1.setText(String.format("  x1(carta/14): %.4f", ia.perceptronTruco.getUltimaEntrada1()));
        LblTrucoNet.setText(String.format("  net: %.4f", ia.perceptronTruco.getUltimaSalidaNeta()));
        int st = ia.perceptronTruco.getUltimaSalida();
        LblTrucoSalida.setText("  SALIDA: " + st + (st == 1 ? "  ✔ cartas bravas" : "  ✘ débil"));
        LblTrucoSalida.setForeground(st == 1 ? Color.GREEN : new Color(255, 100, 100));
        LblTrucoError.setText(String.format("  error: %.4f", ia.perceptronTruco.getUltimoError()));

        LblEstadoW0.setText(String.format("  w0: %.4f", ia.perceptronEstado.getW0()));
        LblEstadoW1.setText(String.format("  w1: %.4f", ia.perceptronEstado.getW1()));
        LblEstadoW2.setText(String.format("  w2: %.4f", ia.perceptronEstado.getW2()));
        LblEstadoX1.setText(String.format("  x1(+1/-1): %.4f", ia.perceptronEstado.getUltimaEntrada1()));
        LblEstadoNet.setText(String.format("  net: %.4f", ia.perceptronEstado.getUltimaSalidaNeta()));
        int sse = ia.perceptronEstado.getUltimaSalida();
        LblEstadoSalida.setText("  SALIDA: " + sse + (sse == 1 ? "  ✔ con ventaja" : "  ✘ sin ventaja"));
        LblEstadoSalida.setForeground(sse == 1 ? Color.GREEN : new Color(255, 100, 100));
        LblEstadoError.setText(String.format("  error: %.4f", ia.perceptronEstado.getUltimoError()));

        int dec = ia.getSalidaDecision();
        LblDecision.setText("  " + (dec == 1 ? "AGRESIVO ⚔" : "CONSERVADOR 🛡"));
        LblDecision.setForeground(dec == 1 ? Color.YELLOW : new Color(190, 190, 190));

        PanelPerceptrones.revalidate();
        PanelPerceptrones.repaint();
    }

    // ── Helpers de componentes ────────────────────────────────────────────────

    private JLabel makeCardLabel() {
        JLabel lbl = new JLabel("", SwingConstants.CENTER);
        lbl.setPreferredSize(new Dimension(CARD_W, CARD_H));
        lbl.setOpaque(true);
        lbl.setBackground(new Color(240, 230, 200));
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 80, 40), 2),
                BorderFactory.createLineBorder(new Color(220, 200, 150), 1)));
        return lbl;
    }

    private JLabel makeCardBackLabel() {
        JLabel lbl = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BACK);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(150, 0, 0));
                for (int y = 0; y < getHeight(); y += 14) {
                    for (int x = (y / 14 % 2 == 0 ? 0 : 7); x < getWidth(); x += 14) {
                        g2.fillRect(x, y, 7, 7);
                    }
                }
                g2.setColor(new Color(255, 220, 150));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
            }
        };
        lbl.setPreferredSize(new Dimension(CARD_W, CARD_H));
        lbl.setOpaque(false);
        return lbl;
    }

    private JLabel makeEmptyCardSlot(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setPreferredSize(new Dimension(CARD_W, CARD_H));
        lbl.setOpaque(true);
        lbl.setBackground(new Color(20, 80, 25));  // sin transparencia — evita artefactos visuales
        lbl.setForeground(new Color(100, 180, 100));
        lbl.setFont(new Font("Georgia", Font.BOLD, 24));
        lbl.setBorder(BorderFactory.createDashedBorder(new Color(80, 150, 80), 4, 4));
        return lbl;
    }

    private JLabel makeScoreLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Georgia", Font.BOLD, 16));
        lbl.setForeground(GOLD);
        lbl.setOpaque(true);
        lbl.setBackground(new Color(0, 0, 0));  // completamente opaco
        lbl.setBorder(BorderFactory.createLineBorder(GOLD_DARK, 1));
        return lbl;
    }

    private JLabel makeSmallLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Georgia", Font.ITALIC, 11));
        lbl.setForeground(new Color(180, 220, 180));
        return lbl;
    }

    private JButton makeCardButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isPressed() ? new Color(180, 130, 20)
                           : getModel().isRollover() ? new Color(230, 190, 60)
                           : new Color(200, 160, 30);
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(GOLD_DARK);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(Color.BLACK);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setFont(new Font("Georgia", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton makeCantarBtn(String text, Color bgColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = !isEnabled() ? new Color(80, 80, 80)
                           : getModel().isPressed() ? bgColor.darker().darker()
                           : getModel().isRollover() ? bgColor.brighter()
                           : bgColor;
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(GOLD_DARK);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(isEnabled() ? Color.WHITE : new Color(160, 160, 160));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setFont(new Font("Georgia", Font.BOLD, 10));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public void limpiarRondas() {
        for (int i = 0; i < 3; i++) {
            SlotRondaJugador[i].setIcon(null);
            SlotRondaJugador[i].setText("");
            SlotRondaJugador[i].setVisible(false);
            SlotRondaIA[i].setIcon(null);
            SlotRondaIA[i].setText("");
            SlotRondaIA[i].setVisible(false);
        }
    }

    public void registrarRondaEnHistorial(int slot, javax.swing.ImageIcon iconoJugador,
                                          String textoJ, javax.swing.ImageIcon iconoIA, String textoI) {
        if (slot < 0 || slot > 2) return;
        int miniW = 52, miniH = 80;
        if (iconoJugador != null) {
            java.awt.Image img = iconoJugador.getImage().getScaledInstance(miniW, miniH, java.awt.Image.SCALE_SMOOTH);
            SlotRondaJugador[slot].setIcon(new javax.swing.ImageIcon(img));
            SlotRondaJugador[slot].setText("");
        } else {
            SlotRondaJugador[slot].setIcon(null);
            SlotRondaJugador[slot].setText(textoJ);
        }
        SlotRondaJugador[slot].setVisible(true);

        if (iconoIA != null) {
            java.awt.Image img = iconoIA.getImage().getScaledInstance(miniW, miniH, java.awt.Image.SCALE_SMOOTH);
            SlotRondaIA[slot].setIcon(new javax.swing.ImageIcon(img));
            SlotRondaIA[slot].setText("");
        } else {
            SlotRondaIA[slot].setIcon(null);
            SlotRondaIA[slot].setText(textoI);
        }
        SlotRondaIA[slot].setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {}
            new InGame().setVisible(true);
        });
    }
}
