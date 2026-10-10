// Funciones para el reporte de Recepción de Hojas
(function () {
    // Se utiliza el modo estricto para evitar errores comunes y mejorar la seguridad del código
    'use strict';
    // Se obtiene el ID de la Hoja desde el atributo data-id-hoja del elemento <main>
    const idHoja = document.querySelector('main[data-id-hoja]')?.dataset.idHoja;
    // Se define un mensaje HTML para mostrar cuando no hay contenedores asociados a la Hoja
    const sinContenedores = '<tr><td colspan="5" class="text-center text-muted py-3">No se encontraron registros históricos para esta Hoja.</td></tr>';
    // Funciones auxiliares para formatear valores de texto, números y fechas
    const texto = valor => valor === null || valor === undefined || valor === '' ? '—' : escapar(valor);
    const numero = valor => Number.isFinite(Number(valor)) ? Number(valor) : 0;
    const fecha = valor => valor ? new Date(valor).toLocaleString('es-MX') : '—';
    // Función para escapar caracteres especiales en HTML y evitar inyecciones de código
    function escapar(valor) { return String(valor ?? '').replace(/[&<>'"]/g, caracter => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'}[caracter])); }
    // Función para mostrar un valor o un mensaje por defecto si el valor es nulo o vacío
    function contenido(valor) { return valor || 'Sin contenedor'; }
    // Función para renderizar los datos de la Hoja en el HTML
    function renderizar(data) {
        document.getElementById('recepcionIdHoja').textContent = texto(data.idHoja);
        document.getElementById('recepcionProyecto').textContent = texto(data.nombreProyecto);
        document.getElementById('recepcionCliente').textContent = texto(data.cliente);
        document.getElementById('recepcionEstado').textContent = texto(data.estado);
        document.getElementById('recepcionFechaRegreso').textContent = fecha(data.fechaEstimadaRegreso);
        document.getElementById('recepcionInicio').textContent = fecha(data.inicioOperacion);
        document.getElementById('recepcionFin').textContent = fecha(data.finOperacion);
        const surtido = numero(data.totalSurtido), devuelto = numero(data.totalDevuelto);
        document.getElementById('recepcionTotalSurtido').textContent = surtido;
        document.getElementById('recepcionTotalDevuelto').textContent = devuelto;
        document.getElementById('recepcionPendiente').textContent = Math.max(surtido - devuelto, 0);
        // Renderizar las tablas de detalles, movimientos y contenedores
        const detalles = data.detalles || [];
        document.getElementById('recepcionProductos').innerHTML = detalles.length ? detalles.map(d => `<tr><td>${texto(d.producto)}</td><td>${numero(d.cantidadSurtida)}</td><td>${numero(d.cantidadDevuelta)}</td><td>${Math.max(numero(d.cantidadSurtida) - numero(d.cantidadDevuelta), 0)}</td></tr>`).join('') : '<tr><td colspan="4" class="text-center text-muted py-3">No se encontraron productos para esta Hoja.</td></tr>';
        const movimientos = data.movimientos || [];
        document.getElementById('recepcionMovimientos').innerHTML = movimientos.length ? movimientos.map(m => `<tr><td>${fecha(m.fecha)}</td><td>${texto(m.codigoConjunto)}</td><td>${texto(m.producto)}</td><td>${texto(contenido(m.codigoContenedor))}</td><td>${texto(m.usuario)}</td><td>${texto(m.observaciones)}</td></tr>`).join('') : '<tr><td colspan="6" class="text-center text-muted py-3">No se encontraron registros históricos para esta Hoja.</td></tr>';
        const contenedores = data.contenedores || [];
        document.getElementById('recepcionContenedores').innerHTML = contenedores.length ? contenedores.map(c => `<tr><td>${texto(c.codigoContenedor)}</td><td>${fecha(c.fechaAsignacion)}<br><small>${texto(c.usuarioAsignacion)}</small></td><td>${fecha(c.fechaCierreCarga)}<br><small>${texto(c.usuarioCierre)}</small></td><td>${fecha(c.fechaLiberacion)}<br><small>${texto(c.usuarioLiberacion)}</small></td><td>${texto(c.usuarioAsignacion)}${c.usuarioCierre ? ` / ${texto(c.usuarioCierre)}` : ''}${c.usuarioLiberacion ? ` / ${texto(c.usuarioLiberacion)}` : ''}</td></tr>`).join('') : sinContenedores;
    }
    // Función para cargar los datos de la Hoja desde la API y renderizarlos en el HTML
    async function cargar() {
        // Si no hay un ID de Hoja, no se realiza la consulta
        if (!idHoja) return;
        // Se muestra un overlay de carga mientras se realiza la solicitud
        LoadingOverlay.mostrar('Cargando detalle de Recepción...');
        // Se realiza la solicitud a la API para obtener los datos de la Hoja
        try {
            const respuesta = await fetch(`/reportes/api/hojas/${encodeURIComponent(idHoja)}/recepcion`, {headers: {'Accept': 'application/json'}});
            // Se intenta obtener el cuerpo de la respuesta como JSON, si es posible
            let cuerpo = null; try { cuerpo = await respuesta.json(); } catch (_) { /* respuesta sin JSON */ }
            // Se manejan los códigos de estado HTTP para redirigir o mostrar errores según corresponda
            if (respuesta.status === 401) { window.location.href = '/login'; return; }
            if (respuesta.status === 403) throw new Error('No tienes permisos para consultar este reporte.');
            if (respuesta.status === 404) throw new Error(cuerpo?.mensaje || 'La Hoja no existe o no está disponible.');
            if (!respuesta.ok) throw new Error('No fue posible consultar el detalle de Recepción.');
            renderizar(cuerpo || {});
        } catch (error) {
            // Se muestra un mensaje de error si ocurre algún problema durante la consulta
            mostrarError(error.message, 'Error de consulta');
        } finally { await LoadingOverlay.ocultar(); }
    }
    // Función para conectar los botones de exportación a Excel y PDF con sus respectivas acciones
    function conectarExportacion() {
        // Se agregan los event listeners a los botones de exportación, si existen en el DOM
        document.getElementById('exportarRecepcionExcel')?.addEventListener('click', () => {
            // Se verifica que exista un ID de Hoja y que la función ReportesExcel esté disponible antes de intentar descargar
            if (!idHoja || !window.ReportesExcel) return;
            ReportesExcel.descargar(
                `/reportes/hojas/${encodeURIComponent(idHoja)}/recepcion/excel`,
                null,
                `SGREA_Hoja_${idHoja}_Recepcion.xlsx`,
                'Generando Excel de Recepción...'
            );
        });
        document.getElementById('exportarRecepcionPdf')?.addEventListener('click', () => {
            // Se verifica que exista un ID de Hoja y que la función ReportesPdf esté disponible antes de intentar descargar
            if (!idHoja || !window.ReportesPdf) return;
            ReportesPdf.descargar(
                `/reportes/hojas/${encodeURIComponent(idHoja)}/recepcion/pdf`,
                null,
                `SGREA_Hoja_${idHoja}_Recepcion.pdf`,
                'Generando PDF de Recepción...'
            );
        });
    }
    // Se ejecuta la función de conexión de exportación y la carga de datos cuando el DOM está completamente cargado
    document.addEventListener('DOMContentLoaded', () => {
        conectarExportacion();
        cargar();
    });
})();
