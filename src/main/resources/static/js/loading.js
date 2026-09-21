const LoadingOverlay = (() => {
    let operacionesActivas = 0;
    let temporizadorOcultar = null;

    function mostrar(mensaje = 'Cargando...') {
        const overlay = document.getElementById('appLoadingOverlay');
        const texto = document.getElementById('appLoadingText');
        if (!overlay) return;
        clearTimeout(temporizadorOcultar);
        operacionesActivas++;
        if (texto) texto.textContent = mensaje;
        overlay.classList.remove('d-none');
        document.body.setAttribute('aria-busy', 'true');
    }

    function ocultar() {
        const overlay = document.getElementById('appLoadingOverlay');
        if (!overlay) return Promise.resolve();
        operacionesActivas = Math.max(0, operacionesActivas - 1);
        if (operacionesActivas === 0) {
            return new Promise(resolve => {
                temporizadorOcultar = setTimeout(() => {
                    overlay.classList.add('d-none');
                    document.body.removeAttribute('aria-busy');
                    resolve();
                }, 500);
            });
        }
        return Promise.resolve();
    }

    async function ejecutar(operacion, mensaje = 'Cargando...') {
        mostrar(mensaje);
        try {
            return await operacion();
        } finally {
            await ocultar();
        }
    }

    return { mostrar, ocultar, ejecutar };
})();
