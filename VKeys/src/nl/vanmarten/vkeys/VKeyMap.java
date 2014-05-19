/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.vanmarten.vkeys;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGElementException;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.ShapeElement;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.xml.StyleAttribute;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.beans.Beans;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.Caret;

/**
 *
 * @author martenjacobs
 */
public class VKeyMap extends JPanel{
    public static SVGUniverse svg = new SVGUniverse();
    
    private final String LAYER_CAPSLOCK="capslock";
    private final String LAYER_SHIFT="shift";
    private final String LAYER_ALT="alt";
    private final String LAYER_NORMAL="normal";
    
    private final String[] LAYERTYPES=new String[]{LAYER_CAPSLOCK,LAYER_SHIFT,LAYER_ALT,LAYER_NORMAL};
    
    private SVGDiagram currentmap = null;
    private double xscale = 0;
    private double yscale = 0;
    private ArrayList<SpecialKeyPressListener> listeners = new ArrayList();
    
    private JTextField tf = null;

    public VKeyMap(){
        if(java.beans.Beans.isDesignTime()) return;
        //this.setBackground(new Color(0, 0, 180));
        if (Beans.isDesignTime()) return;
        this.addMouseListener(panelMouseListener);
    }
    
    public synchronized void loadKeyMap(String f) throws FileNotFoundException{
        loadKeyMap(ClassLoader.getSystemResourceAsStream(f+".svg"), "builtin-"+f);
    }
    public synchronized void loadKeyMap(InputStream f, String name) throws FileNotFoundException{
        //System.out.println("f : " + f);
        //System.out.println("svg : " + svg);
        URI mapUri = svg.loadSVG(new InputStreamReader(f), name);
        currentmap = svg.getDiagram(mapUri);
        showLayer(LAYER_NORMAL);
        repaint();
    }
    public synchronized void loadKeyMap(File f) throws FileNotFoundException{
        URI mapUri = svg.loadSVG(new FileReader(f), f.getAbsolutePath());
        currentmap = svg.getDiagram(mapUri);
        showLayer(LAYER_NORMAL);
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        try {
            paintSVG(g);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void paintSVG(Graphics g) throws SVGException {
        if (currentmap == null) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        SVGDiagram map = currentmap;

        //map.getRoot().
        
        AffineTransform oldTransform = g2d.getTransform();
        xscale = ((double) this.getWidth()) / ((double) map.getViewRect().getWidth());
        yscale = ((double) this.getHeight()) / ((double) map.getViewRect().getHeight());
        g2d.scale(xscale, yscale);
        //Font f = new Font();
        
        map.render(g2d);
        g2d.setTransform(oldTransform);

        g.setClip(0, 0, this.getWidth(), this.getHeight());

        
        
    }

    public void addSpecialKeyPressListener(SpecialKeyPressListener l) {
        listeners.add(l);
    }

    public void removeSpecialKeyPressListener(SpecialKeyPressListener l) {
        listeners.remove(l);
    }
    
    private void KeyMouseActionPerformed(ShapeElement element, MouseEvent me) {
        
        if (me.getButton() != MouseEvent.BUTTON1
                || me.getID() != MouseEvent.MOUSE_CLICKED) {
            return;
        }
        StyleAttribute chr = null;
        chr=element.getPresAbsolute("key-"+currentlayer);
        if(chr!=null){
            typeChar(chr.getStringValue());
        }
        chr = element.getPresAbsolute("key-special");
        if(chr!=null){
            SpecialKeyMouseActionPerformed(chr, me);
        }
    }
    
    private void SpecialKeyMouseActionPerformed(StyleAttribute chr, MouseEvent me) {
        tfFocus();
        String kval = chr.getStringValue();
        if(kval.equals("shift")){
            if(currentlayer.equals(LAYER_NORMAL)){
                showLayer(LAYER_SHIFT);
            }else{
                showLayer(LAYER_NORMAL);
            }
        }else if(kval.equals("capslock")){
            if(currentlayer.equals(LAYER_NORMAL)){
                showLayer(LAYER_CAPSLOCK);
            }else{
                showLayer(LAYER_NORMAL);
            }
        }else if(kval.equals("alt")){
            if(currentlayer.equals(LAYER_NORMAL)){
                showLayer(LAYER_ALT);
            }else{
                showLayer(LAYER_NORMAL);
            }
        }else if(kval.equals("backspace")){
            //sendKey(KeyEvent.VK_BACK_SPACE);
            //clearTyped();
            backSpaceTyped();
        }else if(kval.equals("tab")){
            sendKey(KeyEvent.VK_TAB);
        }else if(kval.equals("enter")){
            sendKey(KeyEvent.VK_ENTER);
        }else if(kval.equals("left")){
            //sendKey(KeyEvent.VK_LEFT);
            moveCaret(-1);
        }else if(kval.equals("right")){
            //sendKey(KeyEvent.VK_RIGHT);
            moveCaret(1);
        }else if(kval.equals("clear")){
            //sendKey(KeyEvent.VK_RIGHT);
            clearTyped();
        }else if(kval.equals("delete")){
            //sendKey(KeyEvent.VK_RIGHT);
            deleteTyped();
        }else if(kval.equals("decimal")){
            //sendKey(KeyEvent.VK_RIGHT);
            sendKey(KeyEvent.VK_DECIMAL);
        }
        
        
    }
    private void tfFocus(){
        if(tf!=null && !tf.hasFocus()){
            tf.requestFocus();
            SwingUtilities.invokeLater(new Runnable(){

                @Override
                public void run() {
                    tf.setCaretPosition(tf.getText().length());
                }
            });
        }
    }
    public void typeChar(String chr){
        if(currentlayer.equals(LAYER_SHIFT) || currentlayer.equals(LAYER_ALT)){
            showLayer(LAYER_NORMAL);
        }
        if(tf!=null){
            tfFocus();
            Caret caret = tf.getCaret();
            int p = caret.getDot();
            String t = tf.getText();
            tf.setText(t.substring(0,p) + chr + t.substring(p));
            caret.setDot(p+1);
        }
    }
    public void backSpaceTyped(){
        if(tf!=null){
            Caret caret = tf.getCaret();
            int p = caret.getDot();
            if(p==0) return;
            String t = tf.getText();
            tf.setText(t.substring(0,p-1) + t.substring(p));
            caret.setDot(p-1);
        }
    }
    public void deleteTyped(){
        if(tf!=null){
            Caret caret = tf.getCaret();
            int p = caret.getDot();
            String t = tf.getText();
            if(p==t.length()) return;
            tf.setText(t.substring(0,p) + t.substring(p+1));
            caret.setDot(p);
        }
    }
    public void clearTyped(){
        if(tf!=null){
            tf.setText("");
        }
    }
    public void moveCaret(int direction){
        if(tf!=null){
            Caret caret = tf.getCaret();
            int p = caret.getDot();
            caret.setDot(p+direction);
        }
    }
    private void sendKey(int key){
        synchronized(listeners){
            for(SpecialKeyPressListener l : listeners){
                l.keyTyped(this, key);
            }
        }
    }
/*
    private void performFunction(ShapeElement element, String function, String[] attributes) {
        String type = element.getPresAbsolute("vanmarten-type").getStringValue();
        ShapeElement[] items = getElementsOfType(type);
        /*for (ShapeElement item : items) {
            setFill(item, Color.yellow);
        }
        setFill(element, Color.red);
        for (FunctionListener l : listeners) {
            l.functionPerformed(element, function, attributes);
        }
    }*/

    public void setFill(ShapeElement s, Color c) {
        try {
            //System.out.println("Set color");
            if(!s.hasAttribute("origfill", AnimationElement.AT_XML)){
                StyleAttribute col = s.getPresAbsolute("fill");
                if(col==null){
                    s.addAttribute("origfill", AnimationElement.AT_XML, "");
                }else{
                    s.addAttribute("origfill", AnimationElement.AT_XML, col.getStringValue());
                }
            }
            if(c==null){
                String orig = s.getPresAbsolute("origfill").getStringValue();
                if(orig.equals("")){
                    s.removeAttribute("fill", AnimationElement.AT_XML);
                }else{
                    s.setAttribute("fill", AnimationElement.AT_XML, orig);
                }
            }else if(!s.hasAttribute("fill", AnimationElement.AT_XML)){
                s.addAttribute("fill", AnimationElement.AT_XML, "#" + Integer.toHexString(c.getRGB()));
            }else{
                s.setAttribute("fill", AnimationElement.AT_XML, "#" + Integer.toHexString(c.getRGB()));
            }
            repaint();
        } catch (SVGException e) {
            e.printStackTrace();
        }
    }

    public ShapeElement[] getElementsOfType(String type) {
        ArrayList<ShapeElement> a = new ArrayList();
        getElementsOfType(type, a);
        ShapeElement[] ts = new ShapeElement[a.size()];
        return a.toArray(ts);
    }

    private void getElementsOfType(String type, ArrayList<ShapeElement> a) {
        if(currentmap==null) return;
        getElementsOfType(type, a, currentmap.getRoot());
    }

    private void getElementsOfType(String type, ArrayList<ShapeElement> a, SVGElement parent) {
        if (parent instanceof ShapeElement) {
            try {
                String eltype = parent.getPresAbsolute("vanmarten-type").getStringValue();
                if (eltype.equals(type)) {
                    a.add((ShapeElement) parent);
                }
            } catch (Exception e) {
            }
        }
        List l = parent.getChildren(null);
        for (Object o : l) {
            if (o instanceof SVGElement) {
                getElementsOfType(type, a, (SVGElement) o);
            }
        }
    }
    private final MouseListener panelMouseListener = new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent me) {
            if (!(me.getButton() == MouseEvent.BUTTON1
                    && me.getID() == MouseEvent.MOUSE_CLICKED)) {
                return;
            }
            ShapeElement[] el = getClickedKeys(me);
            for(ShapeElement s:el){
                KeyMouseActionPerformed(s,me);
            }
        }

        @Override
        public void mousePressed(MouseEvent me) {
             if (!(me.getButton() == MouseEvent.BUTTON1
                    && me.getID() == MouseEvent.MOUSE_PRESSED)) {
                return;
            }
           ShapeElement[] el = getClickedKeys(me);
            for(ShapeElement s:el){
                KeyMouseActionPerformed(s,me);
                setFill(s,Color.lightGray);
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            if (!(me.getButton() == MouseEvent.BUTTON1
                    && me.getID() == MouseEvent.MOUSE_RELEASED)) {
                return;
            }
            ShapeElement[] el = getClickedKeys(me);
            for(ShapeElement s:el){
                KeyMouseActionPerformed(s,me);
            }
            SVGElement[] els = findSvgElements(new Invoke<Boolean, SVGElement>(){

                @Override
                public Boolean call(SVGElement data) {
                    if(!(data instanceof ShapeElement)) return false;
                    ShapeElement se = (ShapeElement) data;
                    if(se.getPresAbsolute("key-"+currentlayer) == null &&
                            se.getPresAbsolute("key-special") == null){
                        return false;
                    }
                    return true;
                }
            });
            for(SVGElement se:els){
                ShapeElement s = (ShapeElement) se;
                setFill(s,null);
            }
        }

        @Override
        public void mouseEntered(MouseEvent me) {
        }

        @Override
        public void mouseExited(MouseEvent me) {
        }
    };

    private ShapeElement[] getClickedNormalKeys(MouseEvent me) {
        return getClickedElements(me, new Invoke<Boolean,SVGElement>(){

            @Override
            public Boolean call(SVGElement element) {
                try {
                    return element.hasAttribute("key-"+currentlayer,
                        AnimationElement.AT_XML);
                } catch (SVGElementException ex) {
                    return false;
                }
            }
        });
    }
    private ShapeElement[] getClickedSpecialKeys(MouseEvent me) {
        return getClickedElements(me, new Invoke<Boolean,SVGElement>(){

            @Override
            public Boolean call(SVGElement element) {
                try {
                    return element.hasAttribute("key-special",
                        AnimationElement.AT_XML);
                } catch (SVGElementException ex) {
                    return false;
                }
            }
        });
    }
    private ShapeElement[] getClickedKeys(MouseEvent me) {
        return getClickedElements(me, new Invoke<Boolean,SVGElement>(){

            @Override
            public Boolean call(SVGElement element) {
                try {
                    return element.hasAttribute("key-"+currentlayer,
                        AnimationElement.AT_XML) || 
                            element.hasAttribute("key-special",
                        AnimationElement.AT_XML);
                } catch (SVGElementException ex) {
                    return false;
                }
            }
        });
    }
    private ShapeElement[] getClickedElements(MouseEvent me, Invoke<Boolean, SVGElement> filter) {
        if (currentmap == null) {
            return new ShapeElement[0];
        }
        ArrayList<ShapeElement> a = new ArrayList<ShapeElement>();
        try {
            ArrayList l = new ArrayList();
            currentmap.pick(new Point(
                    (int) (me.getPoint().x / xscale),
                    (int) (me.getPoint().y / yscale)), l);
            //System.out.println(l);
            for (Object l1 : l) {
                if (!(l1 instanceof List)) {
                    return new ShapeElement[0];
                }
                for (Object i : ((List) l1)) {
                    if (!(i instanceof ShapeElement)) {
                        continue;
                    }
                    ShapeElement element = (ShapeElement) i;
                    if (filter.call(element)) {
                        //System.out.println(element);
                        a.add(element);
                    }
                }
            }
        } catch (SVGException e) {
            e.printStackTrace();
        }
        ShapeElement[] ret = new ShapeElement[a.size()];
        //System.out.println(a);
        return a.toArray(ret);
    }
    String currentlayer="";
    private void showLayer(String layer) {
        if (currentmap == null) {
            return;
        }
        try {
            SVGElement e;
            for(String type : LAYERTYPES){
                e=currentmap.getElement(type);
                if(!e.hasAttribute("visibility",AnimationElement.AT_XML)){
                    e.addAttribute("visibility",AnimationElement.AT_XML,"hidden");
                }else{
                    e.setAttribute("visibility",AnimationElement.AT_XML,"hidden");
                }
            }
            e=currentmap.getElement(layer);
            e.setAttribute("visibility",AnimationElement.AT_XML,"visible");
            currentlayer=layer;
        } catch (SVGElementException ex) {
            ex.printStackTrace();
        }
        repaint();
    }

    public SVGElement[] getKeysForChar(final char kc){
        return findSvgElements(new Invoke<Boolean, SVGElement>() {

            @Override
            public Boolean call(SVGElement data) {
                String d = data.getPresAbsolute("key-"+currentlayer).getStringValue();
                if(d!=null && d.length()>=1){
                    return d.charAt(0)==kc;
                }
                return false;
            }
        });
        
    }
    public SVGElement[] findSvgElements(Invoke<Boolean, SVGElement> filter){
        if (currentmap == null) {
            return new SVGElement[0];
        }
        return findSvgElements(filter, currentmap.getRoot());
    }
    public SVGElement[] findSvgElements(Invoke<Boolean, SVGElement> filter, SVGElement parent){
        ArrayList<SVGElement> l = new ArrayList();
        List<SVGElement> elements = (List<SVGElement>) parent.getChildren(null);
        if(!(elements instanceof List) || elements.size()==0){
            return new SVGElement[0];
        }
        for(SVGElement elem : elements){
            if(filter.call(elem)) l.add(elem);
            l.addAll(Arrays.asList(findSvgElements(filter, elem)));
        }
        
        SVGElement[] elems = new SVGElement[l.size()];
        return l.toArray(elems);
    }

    /**
     * @return the tf
     */
    public JTextField getTextField() {
        return tf;
    }

    /**
     * @param tf the tf to set
     */
    public void setTextField(JTextField tf) {
        this.tf = tf;
        tfFocus();
    }
    /**
     * @param tf the tf to set
     */
    public void clearTextField() {
        this.tf = null;
    }

    
    public interface Invoke<T,V>  {     
        public T call(V data);
    }
}
