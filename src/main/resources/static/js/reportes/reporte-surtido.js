// Funciones para mostrar el detalle de Surtido de una Hoja de Ruta en la sección de Reportes.
(function () {
    // Se utiliza el modo estricto para evitar errores comunes y mejorar la seguridad del código
    'use strict';
    // Se obtiene el ID de la Hoja desde el atributo data-id-hoja del elemento <main>
    const idHoja = document.querySelector('main[data-id-hoja]')?.dataset.idHoja;
    // Se define un mensaje HTML para mostrar cuando no hay movimientos o contenedores asociados a la Hoja
    const sinMovimientos = '<tr><td colspan="6" class="text-center text-muted py-3">No se encontraron registros históricos para esta Hoja.</td></tr>';
    const sinContenedores = '<tr><td colspan="8" class="text-center text-muted py-3">No se encontraron registros históricos para esta Hoja.</td></tr>';
    // Funciones auxiliares para formatear valores de texto, números, fechas y porcentajes
    const texto = valor => valor === null || valor === undefined || valor === '' ? '—' : escapar(valor);
    const numero = valor => Number.isFinite(Number(valor)) ? Number(valor) : 0;
    const fecha = valor => valor ? new Date(valor).toLocaleString('es-MX') : '—';
    const porcentaje = (valor, total) => total > 0 ? `${Math.min(Math.floor((valor * 100) / total), 100)}%` : '0%';
    // Función para escapar caracteres especiales en HTML y evitar inyecciones de código
    function escapar(valor) { return String(valor ?? '').replace(/[&<>'"]/g, caracter => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'}[caracter])); }
    // Función para mostrar un valor o un mensaje por defecto si el valor es nulo o vacío
    function contenido(valor) { return valor || 'Sin contenedor'; }
    // Función para mostrar una fila vacía con un mensaje en una tabla específica
    function filaVacia(id, colspan = 8) { const elemento = document.getElementById(id); if (elemento) elemento.innerHTML = `<tr><td colspan="${colspan}" class="text-center text-muted py-3">No se encontraron registros históricos para esta Hoja.</td></tr>`; }
    // Función para renderizar los datos de la Hoja en el HTML
    function renderizar(data) {
        document.getElementById('surtidoIdHoja').textContent = texto(data.idHoja);
        document.getElementById('surtidoProyecto').textContent = texto(data.nombreProyecto);
        document.getElementById('surtidoCliente').textContent = texto(data.cliente);
        document.getElementById('surtidoEstado').textContent = texto(data.estado);
        document.getElementById('surtidoFechaSalida').textContent = fecha(data.fechaSalida);
        document.getElementById('surtidoInicio').textContent = fecha(data.inicioOperacion);
        document.getElementById('surtidoFin').textContent = fecha(data.finOperacion);
        const solicitado = numero(data.totalSolicitado), surtido = numero(data.totalSurtido);
        document.getElementById('surtidoTotalSolicitado').textContent = solicitado;
        document.getElementById('surtidoTotalSurtido').textContent = surtido;
        document.getElementById('surtidoCumplimiento').textContent = porcentaje(surtido, solicitado);
        // Renderizar las tablas de detalles, movimientos y contenedores
        const detalles = data.detalles || [];
        document.getElementById('surtidoProductos').innerHTML = detalles.length ? detalles.map(d => `<tr><td>${texto(d.producto)}</td><td>${numero(d.cantidadSolicitada)}</td><td>${numero(d.cantidadSurtida)}</td></tr>`).join('') : '<tr><td colspan="3" class="text-center text-muted py-3">No se encontraron productos para esta Hoja.</td></tr>';
        const movimientos = data.movimientos || [];
        document.getElementById('surtidoMovimientos').innerHTML = movimientos.length ? movimientos.map(m => `<tr><td>${fecha(m.fecha)}</td><td>${texto(m.codigoConjunto)}</td><td>${texto(m.producto)}</td><td>${texto(contenido(m.codigoContenedor))}</td><td>${texto(m.usuario)}</td><td>${texto(m.observaciones)}</td></tr>`).join('') : sinMovimientos;
        const contenedores = data.contenedores || [];
        document.getElementById('surtidoContenedores').innerHTML = contenedores.length ? contenedores.map(c => `<tr><td>${texto(c.codigoContenedor)}</td><td>${numero(c.capacidad)}</td><td>${numero(c.cantidadSalidas)}</td><td>${numero(c.porcentajeUtilizacion)}%</td><td>${fecha(c.fechaAsignacion)}</td><td>${texto(c.usuarioAsignacion)}</td><td>${fecha(c.fechaCierreCarga)}</td><td>${texto(c.usuarioCierre)}</td></tr>`).join('') : sinContenedores;
    }
    // Función para cargar los datos de la Hoja desde la API y renderizarlos en el HTML
    async function cargar() {
        // Si no hay un ID de Hoja, no se realiza la consulta
        if (!idHoja) return;
        LoadingOverlay.mostrar('Cargando detalle de Surtido...');
        try {
            // Se realiza la solicitud a la API para obtener los datos de la Hoja
            const respuesta = await fetch(`/reportes/api/hojas/${encodeURIComponent(idHoja)}/surtido`, {headers: {'Accept': 'application/json'}});
            // Se intenta obtener el cuerpo de la respuesta como JSON, si es posible
            let cuerpo = null; try { cuerpo = await respuesta.json(); } catch (_) { /* respuesta sin JSON */ }
            // Se manejan los códigos de estado HTTP para redirigir o mostrar errores según corresponda
            if (respuesta.status === 401) { window.location.href = '/login'; return; }
            if (respuesta.status === 403) throw new Error('No tienes permisos para consultar este reporte.');
            if (respuesta.status === 404) throw new Error(cuerpo?.mensaje || 'La Hoja no existe o no está disponible.');
            if (!respuesta.ok) throw new Error('No fue posible consultar el detalle de Surtido.');
            // Se llama a la función renderizar para actualizar el HTML con los datos obtenidos
            renderizar(cuerpo || {});
        } catch (error) {
            mostrarError(error.message, 'Error de consulta');
        } finally { await LoadingOverlay.ocultar(); }
    }
    // Función para conectar los botones de exportación a Excel y PDF con sus respectivas acciones
    function conectarExportacion() {
        // Se agregan los event listeners a los botones de exportación, verificando que existan y que el ID de la Hoja esté disponible
        document.getElementById('exportarSurtidoExcel')?.addEventListener('click', () => {
            // Se verifica que exista un ID de Hoja y que la función ReportesExcel esté disponible antes de intentar descargar
            if (!idHoja || !window.ReportesExcel) return;
            ReportesExcel.descargar(
                `/reportes/hojas/${encodeURIComponent(idHoja)}/surtido/excel`,
                null,
                `SGREA_Hoja_${idHoja}_Surtido.xlsx`,
                'Generando Excel de Surtido...'
            );
        });
        // Se agrega el event listener para el botón de exportación a PDF, verificando que exista y que el ID de la Hoja esté disponible
        document.getElementById('exportarSurtidoPdf')?.addEventListener('click', () => {
            // Se verifica que exista un ID de Hoja y que la función ReportesPdf esté disponible antes de intentar descargar
            if (!idHoja || !window.ReportesPdf) return;
            ReportesPdf.descargar(
                `/reportes/hojas/${encodeURIComponent(idHoja)}/surtido/pdf`,
                null,
                `SGREA_Hoja_${idHoja}_Surtido.pdf`,
                'Generando PDF de Surtido...'
            );
        });
    }
    // Se ejecuta la función conectarExportacion y cargar cuando el DOM esté completamente cargado
    document.addEventListener('DOMContentLoaded', () => {
        conectarExportacion();
        cargar();
    });
})();
