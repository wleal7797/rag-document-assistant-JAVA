// Comportamientos ligeros de la interfaz: estado de envío, confirmaciones y atajos.
document.addEventListener('DOMContentLoaded', () => {
    // Indica que el formulario se está procesando y evita envíos dobles.
    document.querySelectorAll('form[data-busy-label]').forEach((form) => {
        form.addEventListener('submit', () => {
            const button = form.querySelector('[data-submit]');
            if (button) {
                button.disabled = true;
                button.textContent = form.dataset.busyLabel;
                button.setAttribute('aria-busy', 'true');
            }
        });
    });

    // Confirmación antes de acciones destructivas.
    document.querySelectorAll('form[data-confirm]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            if (!window.confirm(form.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });

    // Muestra el nombre del archivo elegido.
    const fileInput = document.getElementById('file');
    const fileName = document.querySelector('[data-file-name]');
    if (fileInput && fileName) {
        fileInput.addEventListener('change', () => {
            fileName.textContent = fileInput.files.length ? fileInput.files[0].name : 'Ningún archivo seleccionado';
        });
    }

    // Ctrl + Enter (o Cmd + Enter) envía la pregunta.
    const question = document.getElementById('question');
    if (question) {
        question.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
                event.preventDefault();
                question.form.requestSubmit();
            }
        });
        if (!document.getElementById('respuesta-reciente')) {
            question.focus();
        }
    }
});
