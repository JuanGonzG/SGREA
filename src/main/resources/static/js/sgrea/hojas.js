// Función para escapar caracteres HTML en los valores de las celdas
function escaparHtmlHoja(valor) {
    const caracteres = {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'};
    return String(valor ?? '').replace(/[&<>"']/g, caracter => caracteres[caracter]);
}
// Función para formatear fechas en las celdas
function formatearFechaHoja(cell) {
    const valor = cell.getValue();
    if (!valor) return 'Sin fecha estimada';
    const fecha = new Date(valor);
    // Validar si la fecha es válida
    if (Number.isNaN(fecha.getTime())) return escaparHtmlHoja(valor);
    // Formatear la fecha en el formato deseado (dd/mm/yyyy hh:mm)
    return fecha.toLocaleString('es-MX', {day:'2-digit', month:'2-digit', year:'numeric', hour:'2-digit', minute:'2-digit'});
}
// Función para formatear el estado de la hoja en las celdas
function formatearEstadoHoja(cell) {
    return `<span class="badge text-bg-secondary">${escaparHtmlHoja(cell.getValue()?.nombre || 'Sin estado')}</span>`;
}
// Función para formatear las acciones disponibles en las celdas
function formatearAccionesHoja(cell) {
    // Obtener el estado de la hoja y determinar las acciones disponibles
    const hoja = cell.getRow().getData();
    const estado = hoja.estadoHoja?.idEstadoHoja;
    const acciones = [];
    // Agregar botón de eliminar hoja si el estado es 1
    if (estado === 1) {
        acciones.push(`<button type="button" class="btn btn-sm btn-outline-danger" title="Eliminar hoja" data-accion="eliminar-hoja"><i class="bi bi-trash"></i></button>`);
    }
    // Agregar botón de abrir surtido si el estado es 2 o 3
    if (estado === 2 || estado === 3) {
        acciones.push(`<button type="button" class="btn btn-sm btn-outline-primary" title="Abrir surtido" data-accion="surtir-hoja"><i class="bi bi-upc-scan"></i></button>`);
    }
    // Agregar botón de abrir recepción si el estado es 4 o 5
    if (estado === 4 || estado === 5) {
        acciones.push(`<button type="button" class="btn btn-sm btn-outline-success" title="Abrir recepción" data-accion="recibir-hoja"><i class="bi bi-box-arrow-in-down"></i></button>`);
    }
    return acciones.join('');
}
// Inicializar la tabla de hojas de producción usando Tabulator
document.addEventListener('DOMContentLoaded', () => {
    // Obtener el contenedor de la tabla y verificar si Tabulator está disponible
    const contenedor = document.getElementById('tablaHojas');
    if (!contenedor || typeof Tabulator === 'undefined') return;
    const valoresEstado = {'': 'Todos'};
    // Recorrer las hojas iniciales para obtener los valores de estado y agregarlos al objeto valoresEstado
    (hojasIniciales ?? []).forEach(hoja => {
        const estado = hoja.estadoHoja;
        if (estado?.idEstadoHoja != null) valoresEstado[String(estado.idEstadoHoja)] = estado.nombre || 'Sin estado';
    });
    // Inicializar la tabla con las configuraciones y columnas definidas
    new Tabulator(contenedor, {
        data: hojasIniciales ?? [], layout: 'fitColumns', responsiveLayout: 'collapse', resizableColumns: false,
        placeholder: 'No se encontraron elementos.', pagination: true, paginationMode: 'local', paginationSize: 10,
        paginationSizeSelector: [10, 25, 50, 100],
        // Configuración de los valores por defecto de las columnas, incluyendo alineación y comportamiento al hacer clic en una celda
        columnDefaults: {
            hozAlign:'center', vertAlign:'middle', headerHozAlign:'center',
            // Configuración del evento de clic en una celda para redirigir a la página de detalles de la hoja de producción
            cellClick: (event, cell) => {
                const elemento = event.target instanceof Element ? event.target : null;
                if (elemento?.closest('button')) return;
                window.location.href = `/hojas-produccion/${cell.getRow().getData().idHoja}`;
            }
        },
        // Definición de las columnas de la tabla, incluyendo títulos, campos, filtros y formateadores personalizados
        columns: [
            {title:'ID', field:'idHoja', width:80},
            {title:'Proyecto', field:'nombreProyecto', headerFilter:'input', headerFilterPlaceholder:'Buscar proyecto...', formatter:cell => `<strong>${escaparHtmlHoja(cell.getValue())}</strong>`, minWidth:180, hozAlign:'left', headerHozAlign:'left'},
            {title:'Cliente', field:'cliente', headerFilter:'input', headerFilterPlaceholder:'Buscar cliente...', minWidth:150, hozAlign:'left', headerHozAlign:'left'},
            {title:'Fecha de salida', field:'fechaSalida', formatter:formatearFechaHoja, width:170},
            {title:'Regreso estimado', field:'fechaEstimadaRegreso', formatter:formatearFechaHoja, width:180},
            {title:'Estado', field:'estadoHoja', formatter:formatearEstadoHoja, headerFilter:'list', headerFilterParams:{values:valoresEstado}, headerFilterFunc:(f,v) => !f || String(v?.idEstadoHoja) === f, width:140},
            // Columna de acciones con botones para eliminar o abrir surtido según el estado de la hoja
            {title:'Acciones', formatter:formatearAccionesHoja, headerSort:false, cellClick:async (event, cell) => {
                // Obtener la acción del botón clickeado y verificar si es válida
                const accion = event.target.closest('button')?.dataset.accion;
                if (!accion) return;
                const hoja = cell.getRow().getData();
                // Redirigir a la página de surtido si la acción es 'surtir-hoja'
                if (accion === 'surtir-hoja') {
                    window.location.href = `/hh/surtido/${hoja.idHoja}`;
                    return;
                }
                // Redirigir a la página de recepción si la acción es 'recibir-hoja'
                if (accion === 'recibir-hoja') {
                    window.location.href = `/hh/recepcion/${hoja.idHoja}`;
                    return;
                }
                // Manejar la acción de eliminar hoja de producción
                if (accion !== 'eliminar-hoja') return;
                // Confirmar la acción con el usuario antes de eliminar la hoja
                const confirmacion = await confirmarAccion({titulo:'¿Eliminar hoja de producción?', mensaje:'Esta acción eliminará también sus detalles.', textoConfirmar:'Sí, eliminar', colorConfirmar:'#dc3545'});
                if (!confirmacion.isConfirmed) return;
                // Enviar la solicitud al servidor para eliminar la hoja de producción
                try {
                    // Mostrar un overlay de carga mientras se realiza la solicitud
                    LoadingOverlay.mostrar('Eliminando hoja...');
                    // Realizar la solicitud DELETE al servidor para eliminar la hoja de producción
                    const response = await fetch(`/hojas-produccion/${hoja.idHoja}`, {method:'DELETE'});
                    const data = await leerRespuestaJson(response);
                    // Validar la respuesta del servidor y mostrar un mensaje de éxito o error según corresponda
                    if (!response.ok) throw new Error(data.mensaje || 'No fue posible eliminar la hoja de producción.');
                    await LoadingOverlay.ocultar();
                    await mostrarExito(data.mensaje || 'Hoja eliminada correctamente.', 'Hoja eliminada');
                    window.location.reload();
                } catch (error) {
                    await LoadingOverlay.ocultar();
                    mostrarError(error.message || 'No fue posible eliminar la hoja de producción.');
                }
            }, width:150}
        ],
        locale:'es', langs:{es:{data:{loading:'Cargando...',error:'Error al cargar'},pagination:{page_size:'Filas por página',page_title:'Mostrar página',first:'Primera',first_title:'Primera página',last:'Última',last_title:'Última página',prev:'Anterior',prev_title:'Página anterior',next:'Siguiente',next_title:'Siguiente página',all:'Todos',counter:{showing:'Mostrando',of:'de',rows:'filas',pages:'páginas'}},headerFilters:{default:'Filtrar columna...'}}}
    });
});
// Función para leer la respuesta JSON del servidor y manejar el caso de sesión expirada
async function leerRespuestaJson(response) {
    if (response.status === 401) { window.location.href = '/login'; throw new Error('La sesión ha expirado.'); }
    return (response.headers.get('content-type') || '').includes('application/json') ? response.json() : {};
}
