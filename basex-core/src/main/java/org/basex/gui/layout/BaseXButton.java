package org.basex.gui.layout;

import static org.basex.core.Text.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.basex.gui.*;

/**
 * Project specific button implementation.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class BaseXButton extends JButton {
  /** Button template. */
  private static final AbstractButton TEMPLATE = style(new JToggleButton());

  /**
   * Constructor for text buttons.
   * @param win parent window
   * @param label button label
   */
  public BaseXButton(final Window win, final String label) {
    super(label);

    BaseXLayout.addInteraction(this, win);
    if(!(win instanceof BaseXDialog)) return;

    final BaseXDialog d = (BaseXDialog) win;
    addActionListener(e -> {
      final String text = getText();
      if(text.equals(B_CANCEL)) d.cancel();
      else if(text.equals(B_OK)) d.close();
      else d.action(e.getSource());
    });
    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(final KeyEvent e) {
        if(BaseXKeys.ESCAPE.is(e)) {
          d.cancel();
        } else if(BaseXKeys.NEXTCHAR.is(e) || BaseXKeys.NEXTLINE.is(e)) {
          transferFocus();
        } else if(BaseXKeys.PREVCHAR.is(e) || BaseXKeys.PREVLINE.is(e)) {
          transferFocusBackward();
        }
      }
    });
    BaseXLayout.setMnemonic(this, d.mnem);
  }

  /**
   * Returns a new image button.
   * @param icon name of image icon
   * @param toggle toggle flag
   * @param tooltip tooltip text
   * @param gui reference to the main window
   * @return button
   */
  public static AbstractButton get(final String icon, final String tooltip, final boolean toggle,
      final GUI gui) {

    final AbstractButton button = toggle ? new JToggleButton() : new JButton();
    init(button, icon, tooltip, gui);
    if(!toggle) {
      button.setBorder(TEMPLATE.getBorder());
      button.setMargin(TEMPLATE.getMargin());
    }
    return button;
  }

  /**
   * Initializes an image button.
   * @param button button reference
   * @param icon name of image icon
   * @param tooltip tooltip text
   * @param gui reference to the main window
   */
  private static void init(final AbstractButton button, final String icon, final String tooltip,
      final GUI gui) {

    button.setIcon(BaseXImages.icon(icon));
    BaseXLayout.addInteraction(button, gui);
    if(tooltip != null) button.setToolTipText(tooltip);
    style(button);
  }

  /**
   * Unifies the button style.
   * @param button button reference
   * @return button
   */
  private static AbstractButton style(final AbstractButton button) {
    // no shadow effects (flat style)
    button.setOpaque(false);
    // trim horizontal button margins (mac)
    final Insets in = button.getMargin();
    in.left /= 4;
    in.right /= 4;
    if(in.top < in.left) button.setMargin(in);
    return button;
  }

  /**
   * Creates a new image button for the specified command.
   * @param cmd command
   * @param gui reference to the main window
   * @return button
   */
  public static AbstractButton command(final GUICommand cmd, final GUI gui) {
    final String name = cmd.toString().toLowerCase(Locale.ENGLISH);
    final AbstractButton button = get(name, cmd.shortCut(), cmd.toggle(), gui);
    button.addActionListener(e -> cmd.execute(gui));
    return button;
  }

  @Override
  public void setEnabled(final boolean flag) {
    // skip repainting
    if(flag != isEnabled()) super.setEnabled(flag);
  }
}
