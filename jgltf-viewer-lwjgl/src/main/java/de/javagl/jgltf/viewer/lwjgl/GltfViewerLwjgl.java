/*
 * www.javagl.de - JglTF
 *
 * Copyright 2015-2016 Marco Hutter - http://www.javagl.de
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package de.javagl.jgltf.viewer.lwjgl;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.viewer.AbstractGltfViewer;
import de.javagl.jgltf.viewer.GlContext;
import de.javagl.jgltf.viewer.GltfViewer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URI;
import java.util.logging.Logger;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.opengl.GL11.*;

/**
 * Implementation of a glTF viewer based on LWJGL
 */
public class GltfViewerLwjgl extends AbstractGltfViewer<Component>
{

    public static void main(String[] args) throws Exception
    {
        String uriString =
                "https://github.com/KhronosGroup/glTF-Sample-Models/raw/refs/heads/main/1.0/Duck/glTF/Duck.gltf";
        GltfModelReader r = new GltfModelReader();
        GltfModel gltfModel = r.read(new URI(uriString));
        SwingUtilities.invokeLater(() -> createAndShowGui(gltfModel));
    }

    private static void createAndShowGui(GltfModel gltfModel)
    {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLayout(new BorderLayout());
        f.setPreferredSize(new Dimension(1000,1000));

        // Create a viewer based on JOGL or LWJGL:
        GltfViewer<Component> gltfViewer = new GltfViewerLwjgl();


        gltfViewer.addGltfModel(gltfModel);
        f.getContentPane().add(gltfViewer.getRenderComponent(), BorderLayout.CENTER);
        f.setLocationRelativeTo(null);
        f.pack();
        f.setVisible(true);
        f.transferFocus();
        Runnable renderLoop = new Runnable() {
            @Override
            public void run() {
                if (!gltfViewer.getRenderComponent().isValid()) {
                    GL.setCapabilities(null);
                    return;
                }
                ((AWTGLCanvas ) gltfViewer.getRenderComponent()).render();
                SwingUtilities.invokeLater(this);
            }
        };
        SwingUtilities.invokeLater(renderLoop);
    }

    /**
     * The logger used in this class
     */
    private static final Logger logger =
        Logger.getLogger(GltfViewerLwjgl.class.getName());
    
    /**
     * The AWTGLCanvas, i.e. the rendering component of this renderer
     */
    private Component glComponent;
    
    /**
     * The {@link GlContext}
     */
    private final GlContextLwjgl glContext;

    /**
     * Whether the component was resized, and glViewport has to be called
     */
    private boolean viewportNeedsUpdate = true;  
    
    /**
     * Creates a new GltfViewerJogl
     */
    public GltfViewerLwjgl()
    {
        try
        {
            GLData data = new GLData();
            data.majorVersion = 3;
            data.minorVersion = 3;
            data.profile = GLData.Profile.CORE;
            data.samples = 4;
            this.glComponent = new AWTGLCanvas(data)
            {
                /**
                 * Serial UID
                 */
                private static final long serialVersionUID = 1L;

                @Override
                public void initGL() {
                    System.out.println("OpenGL version: " + effective.majorVersion + "." + effective.minorVersion + " (Profile: " + effective.profile + ")");
                    GL.createCapabilities();
                    glClearColor(0.3f, 0.4f, 0.5f, 1);
                }

                @Override
                public void paintGL()
                {
                    if (viewportNeedsUpdate)
                    {
                        glViewport(0, 0, getWidth(), getHeight());
                        viewportNeedsUpdate = false;
                    }
                    doRender();
                    try
                    {
                        swapBuffers();
                    }
                    catch (Exception e)
                    {
                        logger.severe("Could not swap buffers");
                    }
                }
                
            };
            
            this.glComponent.addComponentListener(new ComponentAdapter()
            {
                @Override
                public void componentResized(ComponentEvent e)
                {
                    viewportNeedsUpdate = true;
                }
            });
        }
        catch (Exception e)
        {
            logger.severe("Could not create AWTGLCanvas");
            this.glComponent = new Canvas();
        }
        
        // Without setting the minimum size, the canvas cannot 
        // be resized when it is embedded in a JSplitPane
        this.glComponent.setMinimumSize(new Dimension(10, 10));
        
        this.glContext = new GlContextLwjgl();
    }
    
    @Override
    public GlContext getGlContext()
    {
        return glContext;
    }
    
    @Override
    public Component getRenderComponent()
    {
        return glComponent;
    }
    
    @Override
    public int getWidth()
    {
        return glComponent.getWidth();
    }
    
    @Override
    public int getHeight()
    {
        return glComponent.getHeight();
    }
    
    @Override
    public void triggerRendering()
    {
        if (getRenderComponent() != null)
        {
            getRenderComponent().repaint();
        }
    }
    
    @Override
    protected void prepareRender()
    {
        // Nothing to do here
    }
    
    @Override
    protected void render()
    {
        // Enable the color and depth mask explicitly before calling glClear.
        // When they are not enabled, they will not be cleared!
        glColorMask(true, true, true, true);
        glDepthMask(true); 
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        renderGltfModels();
    }

    

}
