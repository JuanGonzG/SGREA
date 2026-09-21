// Limpiar el formulario de bodega
function limpiarFormularioBodega() {
    const formulario = document.getElementById('formBodega');
    formulario.reset();
    limpiarValidacionesFormulario(formulario);
    document.getElementById('idBodega').value = '';
}

// Mostrar el modal de bodega
function mostrarModalBodega() {
    bootstrap.Modal.getOrCreateInstance(document.getElementById('modalBodega')).show();
}

// Crear una nueva bodega
function nuevaBodega() {
    limpiarFormularioBodega();
    document.getElementById('modalBodegaLabel').textContent = 'Nueva bodega';
    mostrarModalBodega();
}

// Editar una bodega existente
async function editarBodega(idBodega) {
    try {
        LoadingOverlay.mostrar('Cargando bodega...');
        // Fetch the bodega data from the server
        const response = await fetch('/bodega/get/' + idBodega);
        // Validar la respuesta del servidor
        if (!response.ok) throw new Error('No fue posible cargar la bodega.');
        const bodega = await response.json();
        // Llenar el formulario con los datos de la bodega
        limpiarFormularioBodega();
        document.getElementById('modalBodegaLabel').textContent = 'Editar bodega';
        document.getElementById('idBodega').value = bodega.idBodega;
        document.getElementById('nombreBodega').value = bodega.nombre;
        document.getElementById('descripcionBodega').value = bodega.descripcion;
        // Mostrar el modal de bodega
        await LoadingOverlay.ocultar();
        mostrarModalBodega();
    } catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible cargar la bodega');
    }
}

// Guardar o actualizar una bodega
async function guardarBodega() {
    // Validar el formulario
    const formulario = document.getElementById('formBodega');
    if (!formulario.checkValidity()) {
        formulario.classList.add('was-validated');
        return;
    }
    const idBodega = document.getElementById('idBodega').value;
    const esNueva = !idBodega;
    // Confirmar la acción con el usuario
    const confirmacion = await confirmarAccion({
        titulo: esNueva ? '¿Guardar nueva bodega?' : '¿Actualizar bodega?',
        mensaje: esNueva ? 'Se creará la bodega.' : 'Se actualizará la información de la bodega.',
        textoConfirmar: esNueva ? 'Sí, guardar' : 'Sí, actualizar', colorConfirmar: '#0d6efd'
    });
    if (!confirmacion.isConfirmed) return;
    LoadingOverlay.mostrar('Guardando bodega...');
    // Enviar la solicitud al servidor para guardar o actualizar la bodega
    try {
        const response = await fetch('/bodega/guardar', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(
                {
                    idBodega: idBodega || null,
                    nombre: document.getElementById('nombreBodega').value,
                    descripcion: document.getElementById('descripcionBodega').value
                }
            )
        });
        // Validar la respuesta del servidor
        const data = await response.json();
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible guardar la bodega.');
        await LoadingOverlay.ocultar();
        await mostrarExito(data.mensaje, 'Bodega guardada');
        window.location.reload();
    } catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible guardar');
    }
}

// Eliminar una bodega
async function eliminarBodega(idBodega, nombre) {
    // Confirmar la acción con el usuario
    const confirmacion = await confirmarAccion(
        {
            titulo: '¿Deseas eliminar esta bodega?',
            mensaje: `¿Deseas eliminar la bodega "${nombre || 'Sin nombre'}"?`,
            textoConfirmar: 'Sí, eliminar',
            colorConfirmar: '#dc3545'
        }
    );
    if (!confirmacion.isConfirmed) return;
    LoadingOverlay.mostrar('Eliminando bodega...');
    // Enviar la solicitud al servidor para eliminar la bodega
    try {
        const response = await fetch('/bodega/eliminar/' + idBodega, {method: 'DELETE'});
        const data = await response.json();
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible eliminar la bodega.');
        await LoadingOverlay.ocultar(); await mostrarExito(data.mensaje, 'Bodega eliminada'); window.location.reload();
    } catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible eliminar');
    }
}
// Escapar caracteres HTML para evitar inyección de código
function escaparHtmlBodega(valor) {
    const caracteres = {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'};
    return String(valor ?? '').replace(/[&<>"']/g, caracter => caracteres[caracter]);
}

// Inicializar la tabla de bodegas usando Tabulator
function inicializarTablaBodegas() {
    // Obtener el contenedor de la tabla y verificar si Tabulator está disponible
    const contenedor = document.getElementById('tablaBodegas');
    if (!contenedor || typeof Tabulator === 'undefined') return;
    new Tabulator(contenedor, {data: bodegasIniciales ?? [], layout: 'fitColumns', responsiveLayout: 'collapse',
        pagination: true, paginationSize: 10, paginationSizeSelector: [10, 25, 50, 100], placeholder: 'No se encontraron bodegas.',
        columnDefaults: {hozAlign: 'center', vertAlign: 'middle', headerHozAlign: 'center', cellClick: (event, cell) => {
            if (!event.target.closest('button')) editarBodega(cell.getRow().getData().idBodega);
        }},
        columns: [
            {title: 'ID', field: 'idBodega', width: 90},
            {title: 'Bodega', field: 'nombre', headerFilter: 'input', headerFilterPlaceholder: 'Buscar bodega...',
                formatter: cell => `<strong>${escaparHtmlBodega(cell.getValue())}</strong>`, hozAlign: 'left', headerHozAlign: 'left', minWidth: 180},
            {title: 'Descripción', field: 'descripcion', formatter: cell => `<span class="text-muted">${escaparHtmlBodega(cell.getValue())}</span>`,
                hozAlign: 'left', headerHozAlign: 'left', cssClass: 'text-wrap', minWidth: 350},
            {title: 'Borrar', formatter: () => '<button type="button" class="btn btn-sm btn-outline-danger" data-accion="eliminar" aria-label="Eliminar bodega"><i class="bi bi-trash"></i></button>', width: 125,
                cellClick: (event, cell) => { if (event.target.closest('button')) { const b = cell.getRow().getData(); eliminarBodega(b.idBodega, b.nombre); } }}
        ], locale: 'es', langs: {es: {data: {loading: 'Cargando...', error: 'Error al cargar'}, pagination: {
            page_size: 'Filas por página', first: 'Primera', last: 'Última', prev: 'Anterior', next: 'Siguiente', all: 'Todos'
        }, headerFilters: {default: 'Filtrar columna...'}}}
    });
}

document.addEventListener('DOMContentLoaded', inicializarTablaBodegas);
