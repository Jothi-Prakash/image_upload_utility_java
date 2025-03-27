
    let selectedFiles = [];
    let subfolderFilesMap = new Map(); // Stores subfolder -> files mapping
    let subfolderSizeMap = new Map();  // Stores subfolder -> size mapping

    document.getElementById("folderInput").addEventListener("change", function() {
        selectedFiles = Array.from(this.files);
        const subfolderSection = document.getElementById("subfolderSection");
        const subfolderListDiv = document.getElementById("subfolderList");
        const proceedBtn = document.getElementById("proceedBtn");

        if (selectedFiles.length === 0) {
            subfolderSection.classList.add("hidden");
            proceedBtn.classList.add("hidden");
            return;
        }

        // Extract subfolders and their files
        subfolderFilesMap.clear();
        subfolderSizeMap.clear();
        
        selectedFiles.forEach(file => {
            const parts = file.webkitRelativePath.split("/");
            if (parts.length > 1) {  // Ensuring there is a subfolder
                const subfolder = parts[1];  // Get subfolder name
                if (!subfolderFilesMap.has(subfolder)) {
                    subfolderFilesMap.set(subfolder, []);
                    subfolderSizeMap.set(subfolder, 0);
                }
                subfolderFilesMap.get(subfolder).push(file);
                subfolderSizeMap.set(subfolder, subfolderSizeMap.get(subfolder) + file.size);
            }
        });

        updateSubfolderDisplay();
        subfolderSection.classList.remove("hidden");
        proceedBtn.classList.remove("hidden");
    });
	document.getElementById("folderInput").addEventListener("change", function() {
	    selectedFiles = Array.from(this.files);
	    const folderLabel = document.querySelector(".upload-label");
	    
	    if (selectedFiles.length === 0) {
	        folderLabel.innerHTML = '<img src="https://cdn-icons-png.flaticon.com/512/3767/3767084.png" alt="Folder Icon"> Choose Folder';
	        return;
	    }

	    // Extracting folder name
	    const firstFilePath = selectedFiles[0].webkitRelativePath;
	    const folderName = firstFilePath.split("/")[0]; // Get root folder name

	    // Calculating total folder size
	    let totalSize = selectedFiles.reduce((acc, file) => acc + file.size, 0);
	    let sizeText = totalSize < 1024 ? `${totalSize} B` :
	                   totalSize < 1048576 ? `${(totalSize / 1024).toFixed(2)} KB` :
	                   `${(totalSize / 1048576).toFixed(2)} MB`;

	    // Update folder label with name and size
	    folderLabel.innerHTML = `<img src="https://cdn-icons-png.flaticon.com/512/3767/3767084.png" alt="Folder Icon"> ${folderName} (${sizeText})`;

	    updateSubfolderDisplay();
	});


    function updateSubfolderDisplay() {
        const subfolderListDiv = document.getElementById("subfolderList");
        subfolderListDiv.innerHTML = "";

        let leftColumn = document.createElement("div");
        let rightColumn = document.createElement("div");
        leftColumn.className = "subfolder-column";
        rightColumn.className = "subfolder-column";

        let subfolderArray = Array.from(subfolderFilesMap.keys());

        subfolderArray.forEach((subfolder, index) => {
            const fileList = subfolderFilesMap.get(subfolder);
            let totalSize = subfolderSizeMap.get(subfolder);
            
            // Convert size to readable format
            let sizeText = totalSize < 1024 ? `${totalSize} B` :
                           totalSize < 1048576 ? `${(totalSize / 1024).toFixed(2)} KB` :
                           `${(totalSize / 1048576).toFixed(2)} MB`;

            let subDiv = document.createElement("div");
            subDiv.className = "subfolder-item";
            subDiv.innerHTML = `
                <label>
                    <input type="checkbox" class="subfolder-checkbox" value="${subfolder}">
                    <strong>${subfolder}</strong> <span class="size-info">(${sizeText})</span>
                </label>
                <ul class="image-list">
                    ${fileList.map(file => `<li>${file.name}</li>`).join("")}
                </ul>
            `;

            // Distribute subfolders into two columns
            if (index % 2 === 0) {
                leftColumn.appendChild(subDiv);
            } else {
                rightColumn.appendChild(subDiv);
            }
        });

        subfolderListDiv.appendChild(leftColumn);
        subfolderListDiv.appendChild(rightColumn);
    }

    function submitData() {
        const selectedSubfolders = Array.from(document.querySelectorAll(".subfolder-checkbox:checked")).map(cb => cb.value);
        if (selectedSubfolders.length === 0) {
            alert("Please select at least one subfolder.");
            return;
        }

        const formData = new FormData();
        selectedSubfolders.forEach(subfolder => {
            if (subfolderFilesMap.has(subfolder)) {
                subfolderFilesMap.get(subfolder).forEach(file => {
                    formData.append("files", file, file.webkitRelativePath);
                });
            }
        });

        fetch("/processFolder", {
            method: "POST",
            body: formData
        })
        .then(response => response.ok ? alert("Selected subfolders processed successfully!") : alert("Error processing subfolders."))
        .catch(err => {
            console.error(err);
            alert("Error sending data.");
        });
    }

