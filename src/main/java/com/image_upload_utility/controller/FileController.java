package com.image_upload_utility.controller;

import java.io.File;

import javax.swing.JFileChooser;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FileController {
	
	 @GetMapping("/getFolderPath")
	    @ResponseBody
	    public String getFolderPath() {
	        JFileChooser chooser = new JFileChooser();
	        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	        int returnValue = chooser.showOpenDialog(null);

	        if (returnValue == JFileChooser.APPROVE_OPTION) {
	            File selectedFolder = chooser.getSelectedFile();
	            return selectedFolder.getAbsolutePath();
	        }
	        return "";
	    }

}
