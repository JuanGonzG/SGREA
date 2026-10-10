// Módulo para manejar la exportación de reportes a Excel y PDF
(function () {
    // use strict mode para evitar errores silenciosos y mejorar la depuración
    'use strict';
    // Definición de los tipos MIME para Excel y PDF
    const MIME_XLSX = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    const MIME_PDF = 'application/pdf';
    // Función para extraer el nombre del archivo desde la cabecera Content-Disposition de la respuesta
    function nombreArchivo(respuesta, nombreAlternativo) {
        // Se obtiene la cabecera Content-Disposition de la respuesta
        const disposicion = respuesta.headers.get('Content-Disposition') || '';
        // Se intenta extraer el nombre del archivo usando una expresión regular para UTF-8
        const utf8 = disposicion.match(/filename\*=UTF-8''([^;]+)/i);
        // Si se encuentra un nombre en UTF-8, se decodifica y se devuelve
        if (utf8) return decodeURIComponent(utf8[1].replace(/^"|"$/g, ''));
        // Si no se encuentra un nombre en UTF-8, se intenta extraer un nombre simple
        const simple = disposicion.match(/filename="?([^";]+)"?/i);
        // Si se encuentra un nombre simple, se devuelve; de lo contrario, se devuelve el nombre alternativo proporcionado
        return simple ? simple[1] : nombreAlternativo;
    }
    // Función para generar un mensaje de error basado en el código de estado HTTP y el cuerpo de la respuesta
    function mensajeError(cuerpo, status, tipo) {
        if (status === 401) return 'La sesión ha expirado.';
        if (status === 403) return 'No tienes permisos para exportar este reporte.';
        if (status === 404) return cuerpo?.mensaje || 'El reporte no existe o no está disponible.';
        if (status === 400) return cuerpo?.mensaje || 'Los filtros no son válidos.';
        return cuerpo?.mensaje || `No fue posible descargar el archivo ${tipo}.`;
    }
    // Función para descargar un archivo desde una URL con parámetros y manejar la respuesta
    async function descargar(url, parametros, nombreAlternativo, mensaje, mime, tipo) {
        // Se construye la URL final con los parámetros de consulta si se proporcionan
        const query = parametros instanceof URLSearchParams ? parametros.toString() : '';
        const destino = query ? `${url}?${query}` : url;
        // Se muestra un overlay de carga mientras se realiza la solicitud
        LoadingOverlay.mostrar(mensaje);
        try {
            const respuesta = await fetch(destino, {
                credentials: 'same-origin',
                headers: {Accept: mime}
            });
            if (!respuesta.ok) {
                // Se intenta obtener el cuerpo de la respuesta como JSON
                let cuerpo = null;
                try { cuerpo = await respuesta.json(); } catch (_) { /* respuesta no JSON */ }
                if (respuesta.status === 401) {
                    window.location.href = '/login';
                    return;
                }
                throw new Error(mensajeError(cuerpo, respuesta.status, tipo));
            }
            // Se obtiene el contenido de la respuesta como un blob
            const blob = await respuesta.blob();
            // Se crea un enlace temporal para descargar el archivo
            const enlace = document.createElement('a');
            // Se asigna la URL del blob al enlace y se establece el nombre de descarga
            enlace.href = URL.createObjectURL(blob);
            // Se determina el nombre del archivo a partir de la cabecera o del nombre alternativo
            enlace.download = nombreArchivo(respuesta, nombreAlternativo);
            // Se agrega el enlace al DOM, se simula un clic para iniciar la descarga y luego se elimina el enlace
            document.body.appendChild(enlace);
            // Se simula un clic en el enlace para iniciar la descarga
            enlace.click();
            // Se elimina el enlace del DOM y se revoca la URL del blob para liberar memoria
            enlace.remove();
            // Se revoca la URL del blob para liberar memoria
            URL.revokeObjectURL(enlace.href);
        } catch (error) {
            await mostrarError(error.message || `No fue posible descargar el archivo ${tipo}.`, 'Error de exportación');
        } finally {
            await LoadingOverlay.ocultar();
        }
    }
    // Se exponen las funciones de descarga para Excel y PDF en el objeto global window
    window.ReportesExcel = {
        descargar: (url, parametros, nombreAlternativo, mensaje = 'Generando archivo Excel...') =>
            descargar(url, parametros, nombreAlternativo, mensaje, MIME_XLSX, 'Excel')
    };
    window.ReportesPdf = {
        descargar: (url, parametros, nombreAlternativo, mensaje = 'Generando archivo PDF...') =>
            descargar(url, parametros, nombreAlternativo, mensaje, MIME_PDF, 'PDF')
    };
})();
