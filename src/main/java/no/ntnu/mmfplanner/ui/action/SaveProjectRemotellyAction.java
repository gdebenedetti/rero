/*
 * Copyright (C) 2007 Snorre Gylterud, Stein Magnus Jodal, Johannes Knutsen,
 * Erik Bagge Ottesen, Ralf Bjarne Taraldset, and Iterate AS
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 */

package no.ntnu.mmfplanner.ui.action;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.StringWriter;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import no.ntnu.mmfplanner.ui.MainFrame;
import no.ntnu.mmfplanner.util.XmlSerializer;
import nu.xom.Document;
import nu.xom.Element;

import org.json.JSONObject;
import org.json.XML;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

/**
 * Serializes the current project to XML and saves it to disk.
 */
public class SaveProjectRemotellyAction extends MainAbstractAction {

	private Boolean saveOrUpdate = null;
	private static final long serialVersionUID = 1L;
	public static final String ACTION_NAME = "Save or update Project remotelly...";
	public static final int ACTION_MNEMONIC = KeyEvent.VK_S;
	public static final String ACTION_ACCELERATOR = "ctrl S";
	public static final String ACTION_DESCRIPTION = "Save or update the current project";

	public SaveProjectRemotellyAction(MainFrame mainFrame) {
		super(mainFrame, ACTION_NAME, ACTION_MNEMONIC, ACTION_ACCELERATOR, ACTION_DESCRIPTION);
	}

	public void actionPerformed(ActionEvent evt) {
		if (save()) {
			JOptionPane.showMessageDialog(mainFrame, "Project remotely saved or updated successfully.");
		} else {
			JOptionPane.showMessageDialog(mainFrame, "Failed to save or update the project remotely.", "Fail!",
					JOptionPane.WARNING_MESSAGE);
		}
	}

	public boolean save() {
		final JDialog d = new JDialog();
		final boolean save = mainFrame.getProject().getId() == null ? true
				: "".equals(mainFrame.getProject().getId()) ? true : false;

		JPanel p1 = new JPanel(new GridBagLayout());
		p1.add(new JLabel("Please Wait..."), new GridBagConstraints());
		d.getContentPane().add(p1);
		d.setSize(200, 200);
		d.setLocationRelativeTo(mainFrame);
		d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		d.setModal(true);

		SwingWorker<?, ?> worker = new SwingWorker<Boolean, Void>() {
			protected Boolean doInBackground() throws InterruptedException {
				final StringWriter response = new StringWriter();

				Document document = XmlSerializer.workspaceToDocument(mainFrame.getTabPanePanelPlacement(),
						mainFrame.getProject());

				Element root = document.getRootElement();

				JSONObject json = XML.toJSONObject(root.toXML());

				StringRepresentation stringRep = new StringRepresentation(json.toString());
				stringRep.setMediaType(MediaType.APPLICATION_JSON);

				String URL = "https://api.mlab.com/api/1/databases/mmf_planner_db/collections/projectsJSON/";

				// Save or Update
				if (!save) {
					URL = URL + mainFrame.getProject().getId();
				}

				ClientResource clientJSON = new ClientResource(URL);

				Parameter apiKey = new Parameter("apiKey", "r5Kh17D7-6KVNy70vxx-aY20h7_2Pb4Q");
				clientJSON.addQueryParameter(apiKey);
				clientJSON.getReference().addQueryParameter("format", "json");

				// Save or Update
				if (save) {
					clientJSON.setMethod(Method.POST);
					try {
						clientJSON.post(stringRep, MediaType.APPLICATION_JSON).write(response);
					} catch (ResourceException | IOException e) {
						e.printStackTrace();
						return Boolean.FALSE;
					}
				} else {
					clientJSON.setMethod(Method.PUT);
					clientJSON.put(stringRep, MediaType.APPLICATION_JSON);
				}

				if (!clientJSON.getStatus().isSuccess()) {
					return Boolean.FALSE;
				} else {
					if (save) {
						mainFrame.getProject().setId(
								new JSONObject(response.toString()).getJSONObject("_id").getString("$oid"));
					}
					return Boolean.TRUE;
				}
			}

			protected void done() {
				d.dispose();
				try {
					saveOrUpdate = get();
				} catch (Exception e) {
					e.printStackTrace();
					saveOrUpdate = Boolean.FALSE;
				}
			}
		};
		worker.execute();
		d.setVisible(true);

		return saveOrUpdate.booleanValue();
	}
}
