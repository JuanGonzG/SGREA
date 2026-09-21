function escaparHtmlHoja(valor) {
    const caracteres = {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'};
    return String(valor ?? '').replace(/[&<>"']/g, caracter => caracteres[caracter]);
}

function formatearFechaHoja(cell) {
    const valor = cell.getValue();
    if (!valor) return 'Sin fecha estimada';
    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) return escaparHtmlHoja(valor);
    return fecha.toLocaleString('es-MX', {day:'2-digit', month:'2-digit', year:'numeric', hour:'2-digit', minute:'2-digit'});
}

function formatearEstadoHoja(cell) {
    return `<span class="badge text-bg-secondary">${escaparHtmlHoja(cell.getValue()?.nombre || 'Sin estado')}</span>`;
}

function formatearAccionesHoja(cell) {
    const hoja = cell.getRow().getData();
    return hoja.estadoHoja?.idEstadoHoja === 1
        ? `<button type="button" class="btn btn-sm btn-outline-danger" title="Eliminar hoja" data-accion="eliminar-hoja"><i class="bi bi-trash"></i></button>`
        : '';
}

document.addEventListener('DOMContentLoaded', () => {
    const contenedor = document.getElementById('tablaHojas');
    if (!contenedor || typeof Tabulator === 'undefined') return;
    const valoresEstado = {'': 'Todos'};
    (hojasIniciales ?? []).forEach(hoja => {
        const estado = hoja.estadoHoja;
        if (estado?.idEstadoHoja != null) valoresEstado[String(estado.idEstadoHoja)] = estado.nombre || 'Sin estado';
    });

    new Tabulator(contenedor, {
        data: hojasIniciales ?? [], layout: 'fitColumns', responsiveLayout: 'collapse', resizableColumns: false,
        placeholder: 'No se encontraron elementos.', pagination: true, paginationMode: 'local', paginationSize: 10,
        paginationSizeSelector: [10, 25, 50, 100],
        columnDefaults: {
            hozAlign:'center', vertAlign:'middle', headerHozAlign:'center',
            cellClick: (event, cell) => {
                const elemento = event.target instanceof Element ? event.target : null;
                if (elemento?.closest('button')) return;
                window.location.href = `/hojas-produccion/${cell.getRow().getData().idHoja}`;
            }
        },
        columns: [
            {title:'ID', field:'idHoja', width:80},
            {title:'Proyecto', field:'nombreProyecto', headerFilter:'input', headerFilterPlaceholder:'Buscar proyecto...', formatter:cell => `<strong>${escaparHtmlHoja(cell.getValue())}</strong>`, minWidth:180, hozAlign:'left', headerHozAlign:'left'},
            {title:'Cliente', field:'cliente', headerFilter:'input', headerFilterPlaceholder:'Buscar cliente...', minWidth:150, hozAlign:'left', headerHozAlign:'left'},
            {title:'Fecha de salida', field:'fechaSalida', formatter:formatearFechaHoja, width:170},
            {title:'Regreso estimado', field:'fechaEstimadaRegreso', formatter:formatearFechaHoja, width:180},
            {title:'Estado', field:'estadoHoja', formatter:formatearEstadoHoja, headerFilter:'list', headerFilterParams:{values:valoresEstado}, headerFilterFunc:(f,v) => !f || String(v?.idEstadoHoja) === f, width:140},
            {title:'Acciones', formatter:formatearAccionesHoja, headerSort:false, cellClick:async (event, cell) => {
                if (!event.target.closest('button[data-accion="eliminar-hoja"]')) return;
                const hoja = cell.getRow().getData();
                const confirmacion = await confirmarAccion({titulo:'¿Eliminar hoja de producción?', mensaje:'Esta acción eliminará también sus detalles.', textoConfirmar:'Sí, eliminar', colorConfirmar:'#dc3545'});
                if (!confirmacion.isConfirmed) return;
                try {
                    LoadingOverlay.mostrar('Eliminando hoja...');
                    const response = await fetch(`/hojas-produccion/${hoja.idHoja}`, {method:'DELETE'});
                    const data = await leerRespuestaJson(response);
                    if (!response.ok) throw new Error(data.mensaje || 'No fue posible eliminar la hoja de producción.');
                    await LoadingOverlay.ocultar();
                    await mostrarExito(data.mensaje || 'Hoja eliminada correctamente.', 'Hoja eliminada');
                    window.location.reload();
                } catch (error) {
                    await LoadingOverlay.ocultar();
                    mostrarError(error.message || 'No fue posible eliminar la hoja de producción.');
                }
            }, width:110}
        ],
        locale:'es', langs:{es:{data:{loading:'Cargando...',error:'Error al cargar'},pagination:{page_size:'Filas por página',page_title:'Mostrar página',first:'Primera',first_title:'Primera página',last:'Última',last_title:'Última página',prev:'Anterior',prev_title:'Página anterior',next:'Siguiente',next_title:'Siguiente página',all:'Todos',counter:{showing:'Mostrando',of:'de',rows:'filas',pages:'páginas'}},headerFilters:{default:'Filtrar columna...'}}}
    });
});

async function leerRespuestaJson(response) {
    if (response.status === 401) { window.location.href = '/login'; throw new Error('La sesión ha expirado.'); }
    return (response.headers.get('content-type') || '').includes('application/json') ? response.json() : {};
}
