// Variable para guardar el ID del usuario actualmente seleccionado para la gestión de bodegas
let usuarioBodegasId = null;

// Función para escapar caracteres especiales en HTML y prevenir inyecciones de código
function escaparHtmlUsuario(valor) {
    const caracteres = {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'};
    return String(valor ?? '').replace(/[&<>"']/g, caracter => caracteres[caracter]);
}

// Función para cargar la lista de usuarios desde el servidor y renderizarla en la tabla
async function cargarUsuarios() {
    // Realiza una solicitud al endpoint para obtener la lista de usuarios
    const response = await fetch('/usuarios/listar');
    if (!response.ok) throw new Error('No fue posible cargar los usuarios.');
    // Obtiene la respuesta en formato JSON
    const usuarios = await response.json();
    // Mapea cada usuario a una fila de la tabla, escapando los valores para prevenir inyecciones de código
    const filas = usuarios.map(usuario => `<tr class="fila-usuario" data-id="${usuario.idUsuario}"><td>${usuario.idUsuario}</td>
        <td>${escaparHtmlUsuario(usuario.nombre)}</td><td>${escaparHtmlUsuario(usuario.username)}</td>
        <td>${escaparHtmlUsuario(usuario.rol?.nombre)}</td><td>${usuario.activo ? 'Activo' : 'Inactivo'}</td>
        <td>${(usuario.bodegas ?? []).map(b => escaparHtmlUsuario(b.nombre)).join(', ')}</td>
        <td><button class="btn btn-sm btn-outline-danger btn-eliminar-usuario" data-id="${usuario.idUsuario}" data-username="${escaparHtmlUsuario(usuario.username)}" aria-label="Eliminar usuario">
        <i class="bi bi-trash"></i></button></td></tr>`).join('');
    // Inserta las filas generadas en la tabla de usuarios, o un mensaje si no hay usuarios
    document.getElementById('tablaUsuarios').innerHTML = `<table class="table table-hover align-middle"><thead><tr>
        <th>ID</th><th>Nombre</th><th>Username</th><th>Rol</th><th>Estado</th><th>Bodegas</th><th>Acciones</th></tr></thead>
        <tbody>${filas || '<tr><td colspan="7" class="text-center">No hay usuarios.</td></tr>'}</tbody></table>`;
    // Abre el modal de edición al hacer clic en cualquier parte de la fila, excepto en sus acciones
    document.querySelectorAll('.fila-usuario').forEach(fila => fila.addEventListener('click', event => {
        if (event.target.closest('button')) return;
        editarUsuario(Number(fila.dataset.id));
    }));
    // Agrega un event listener a cada botón de "Eliminar" para eliminar el usuario correspondiente
    document.querySelectorAll('.btn-eliminar-usuario').forEach(button => button.addEventListener('click', () =>
        eliminarUsuario(Number(button.dataset.id), button.dataset.username)));
}

// Función para eliminar un usuario
async function eliminarUsuario(idUsuario, username) {
    // Muestra un cuadro de confirmación antes de proceder con la eliminación del usuario
    const confirmacion = await confirmarAccion({
        titulo: '¿Eliminar usuario?',
        mensaje: `Se eliminará el usuario "${username}". Esta acción no se puede deshacer.`,
        textoConfirmar: 'Sí, eliminar',
        colorConfirmar: '#dc3545'
    });
    if (!confirmacion.isConfirmed) return;
    try {
        // Muestra un overlay de carga mientras se realiza la solicitud de eliminación
        LoadingOverlay.mostrar('Eliminando usuario...');
        const response = await fetch(`/usuarios/eliminar/${idUsuario}`, {method: 'DELETE'});
        const data = await response.json();
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible eliminar el usuario.');
        // Oculta el overlay de carga, muestra un mensaje de éxito y recarga la lista de usuarios
        await LoadingOverlay.ocultar();
        await mostrarExito(data.mensaje, 'Usuario eliminado');
        // Recarga la lista de usuarios para reflejar los cambios
        await cargarUsuarios();
    } catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible eliminar');
    }
}
// Función para abrir el modal de bodegas de un usuario específico
async function abrirBodegasUsuario(idUsuario, username) {
    // Asigna el ID del usuario actualmente seleccionado para la gestión de bodegas
    usuarioBodegasId = idUsuario;
    // Actualiza el título del modal con el nombre de usuario correspondiente
    document.getElementById('modalBodegasUsuarioLabel').textContent = `Bodegas de ${username}`;
    try {
        // Muestra un overlay de carga mientras se obtienen las bodegas del usuario
        LoadingOverlay.mostrar('Cargando bodegas...');
        const response = await fetch(`/usuarios/${idUsuario}/bodegas`);
        const data = await response.json();
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible cargar las bodegas.');
        // Genera la tabla de bodegas asociadas al usuario, escapando los valores para prevenir inyecciones de código
        document.getElementById('tablaBodegasUsuario').innerHTML = `<table class="table align-middle"><thead><tr><th>Bodega</th><th>Descripción</th><th class="text-center">Asociada</th></tr></thead><tbody>
            ${data.map(b => `<tr><td>${escaparHtmlUsuario(b.nombre)}</td><td>${escaparHtmlUsuario(b.descripcion)}</td><td class="text-center">
            <div class="form-check form-switch d-inline-block"><input class="form-check-input switch-bodega-usuario" type="checkbox" data-id="${b.idBodega}" ${b.asociado ? 'checked' : ''} aria-label="Asociar bodega"></div></td></tr>`).join('')}</tbody></table>`;
        // Oculta el overlay de carga y muestra el modal de bodegas del usuario
        await LoadingOverlay.ocultar();
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalBodegasUsuario')).show();
    } catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible cargar las bodegas');
    }
}
// Función para guardar las asociaciones de bodegas del usuario seleccionado
async function guardarBodegasUsuario() {
    // Obtiene los IDs de las bodegas seleccionadas (checkboxes marcados) y los convierte a números
    const ids = [...document.querySelectorAll('.switch-bodega-usuario:checked')].map(input => Number(input.dataset.id));
    // Verifica que al menos una bodega esté seleccionada antes de proceder
    if (!ids.length) {
     mostrarError('Debe conservar al menos una bodega asociada.', 'Operación no permitida');
     return;
    }
    // Muestra un cuadro de confirmación antes de guardar los cambios
    const confirmacion = await confirmarAccion({titulo: '¿Actualizar asociaciones?', mensaje: 'Se guardará la selección de bodegas del usuario.', textoConfirmar: 'Sí, guardar', colorConfirmar: '#0d6efd'});
    if (!confirmacion.isConfirmed) return;
    try {
        LoadingOverlay.mostrar('Guardando asociaciones...');
        // Realiza una solicitud POST al servidor para actualizar las asociaciones de bodegas del usuario
        const response = await fetch(`/usuarios/${usuarioBodegasId}/bodegas`, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(ids)});
        const data = await response.json();
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible guardar las asociaciones.');
        // Oculta el overlay de carga, cierra el modal y muestra un mensaje de éxito
        await LoadingOverlay.ocultar();
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalBodegasUsuario')).hide();
        await mostrarExito(data.mensaje, 'Asociaciones actualizadas');
        await cargarUsuarios();
    } catch (error) { await LoadingOverlay.ocultar(); mostrarError(error.message, 'No fue posible guardar'); }
}
// Agrega un event listener al DOMContentLoaded para inicializar la funcionalidad de la página
document.addEventListener('DOMContentLoaded', () => {
    // Agrega un event listener al botón de guardar bodegas del usuario para llamar a la función correspondiente
    document.getElementById('guardarBodegasUsuario').addEventListener('click', guardarBodegasUsuario);
    document.getElementById('nuevoUsuario').addEventListener('click', abrirModalNuevoUsuario);
    document.getElementById('formUsuario').addEventListener('submit', guardarUsuario);
    // Carga la lista de usuarios al cargar la página y maneja cualquier error que ocurra
    cargarUsuarios().catch(error => mostrarError(error.message, 'No fue posible cargar usuarios'));
});
// Función para limpiar el formulario de usuario y restablecer sus valores
function limpiarFormularioUsuario() {
    const formulario = document.getElementById('formUsuario');
    formulario.reset();
    if (typeof limpiarValidacionesFormulario === 'function') {
        limpiarValidacionesFormulario(formulario);
    } else {
        formulario.classList.remove('was-validated');
        formulario.querySelectorAll('.is-invalid, .is-valid').forEach(campo => campo.classList.remove('is-invalid', 'is-valid'));
        formulario.querySelectorAll('.invalid-tooltip, .invalid-feedback').forEach(mensaje => mensaje.classList.remove('d-block'));
    }
    document.getElementById('idUsuario').value = '';
    document.getElementById('activoUsuario').checked = true;
    document.getElementById('tablaBodegasAlta').innerHTML = '';
    document.getElementById('ayudaBodegasEdicion').classList.add('d-none');
}
// Función para cargar las bodegas disponibles para asociar a un nuevo usuario
async function cargarBodegasParaAlta(seleccionadas = []) {
    // Realiza una solicitud al endpoint para obtener la lista de bodegas disponibles
    const response = await fetch('/usuarios/bodegas');
    const bodegas = await response.json();
    if (!response.ok) throw new Error(bodegas.mensaje || 'No fue posible cargar las bodegas.');
    renderTablaBodegas(bodegas.map(b => ({...b, asociado: seleccionadas.includes(b.idBodega)})), 'switch-bodega-alta', false);
}

function renderTablaBodegas(bodegas, claseSwitch, deshabilitada) {
    document.getElementById('tablaBodegasAlta').innerHTML = `<table class="table table-sm align-middle"><thead><tr><th>Bodega</th><th>Descripción</th><th class="text-center">Asociada</th></tr></thead><tbody>
        ${bodegas.map(b => `<tr><td>${escaparHtmlUsuario(b.nombre)}</td><td>${escaparHtmlUsuario(b.descripcion)}</td><td class="text-center"><div class="form-check form-switch d-inline-block">
        <input class="form-check-input ${claseSwitch}" type="checkbox" data-id="${b.idBodega}" ${b.asociado ? 'checked' : ''} ${deshabilitada ? 'disabled' : ''} aria-label="Asociar bodega"></div></td></tr>`).join('')}</tbody></table>`;
}
// Función para abrir el modal de creación de un nuevo usuario
async function abrirModalNuevoUsuario() {
    limpiarFormularioUsuario();
    document.getElementById('modalUsuarioLabel').textContent = 'Nuevo usuario';
    document.getElementById('passwordUsuario').required = true;
    LoadingOverlay.mostrar('Cargando bodegas...');
    // Carga las bodegas disponibles para asociar al nuevo usuario y maneja cualquier error que ocurra
    try {
        await cargarBodegasParaAlta();
        await LoadingOverlay.ocultar();
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalUsuario')).show();
    }
    catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible abrir el formulario');
    }
}
// Función para abrir el modal de edición de un usuario existente
async function editarUsuario(idUsuario) {
    try {
        // Muestra un overlay de carga mientras se obtiene la información del usuario desde el servidor
        LoadingOverlay.mostrar('Cargando usuario...');
        const response = await fetch(`/usuarios/get/${idUsuario}`);
        const usuario = await response.json();
        if (!response.ok) throw new Error(usuario.mensaje || 'No fue posible cargar el usuario.');
        const responseBodegas = await fetch(`/usuarios/${idUsuario}/bodegas`);
        const bodegas = await responseBodegas.json();
        if (!responseBodegas.ok) throw new Error(bodegas.mensaje || 'No fue posible cargar las bodegas.');
        limpiarFormularioUsuario();
        // Actualiza el título del modal y los campos del formulario con la información del usuario obtenido
        document.getElementById('modalUsuarioLabel').textContent = 'Editar usuario';
        document.getElementById('idUsuario').value = usuario.idUsuario;
        document.getElementById('nombreUsuario').value = usuario.nombre;
        document.getElementById('usernameUsuario').value = usuario.username;
        document.getElementById('rolUsuarioForm').value = usuario.rol.idRol;
        document.getElementById('activoUsuario').checked = usuario.activo;
        document.getElementById('passwordUsuario').required = false;
        renderTablaBodegas(bodegas, 'switch-bodega-edicion', false);
        document.getElementById('ayudaBodegasEdicion').classList.remove('d-none');
        // Oculta el overlay de carga y muestra el modal de edición del usuario
        await LoadingOverlay.ocultar();
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalUsuario')).show();
    } catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible cargar el usuario');
    }
}
// Función para guardar un nuevo usuario o actualizar uno existente
async function guardarUsuario(event) {
    // Previene el comportamiento por defecto del formulario al enviarlo
    event.preventDefault();
    // Valida el formulario y muestra un mensaje de error si no es válido
    const formulario = document.getElementById('formUsuario');
    if (!formulario.checkValidity()) { formulario.classList.add('was-validated'); return; }
    // Obtiene el ID del usuario del formulario y determina si es un nuevo usuario o una actualización
    const id = document.getElementById('idUsuario').value;
    const esNuevo = !id;
    // Obtiene los IDs de las bodegas seleccionadas (checkboxes marcados) y los convierte a números
    const selectorBodegas = esNuevo ? '.switch-bodega-alta:checked' : '.switch-bodega-edicion:checked';
    const idsBodegas = [...document.querySelectorAll(selectorBodegas)].map(input => Number(input.dataset.id));

    // Verifica que al menos una bodega esté seleccionada si es un nuevo usuario
    if (!idsBodegas.length) { mostrarError('Debe conservar al menos una bodega.', 'Formulario incompleto'); return; }

    // Construye el payload con la información del usuario a guardar o actualizar
    const payload = {nombre: document.getElementById('nombreUsuario').value, username: document.getElementById('usernameUsuario').value,
        password: document.getElementById('passwordUsuario').value, activo: document.getElementById('activoUsuario').checked,
        rol: {idRol: Number(document.getElementById('rolUsuarioForm').value)}};

    // Agrega los IDs de bodegas al payload si es un nuevo usuario, o el ID del usuario si es una actualización
    if (esNuevo) payload.idsBodegas = idsBodegas; else payload.idUsuario = Number(id);
    const confirmacion = await confirmarAccion({titulo: esNuevo ? '¿Guardar usuario?' : '¿Actualizar usuario?', mensaje: 'Se guardará la información capturada.', textoConfirmar: esNuevo ? 'Sí, guardar' : 'Sí, actualizar', colorConfirmar: '#0d6efd'});
    if (!confirmacion.isConfirmed) return;
    try {
        // Muestra un overlay de carga mientras se realiza la solicitud de guardado o actualización del usuario
        LoadingOverlay.mostrar('Guardando usuario...');
        const response = await fetch(esNuevo ? '/usuarios/guardar' : '/usuarios/actualizar', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)});
        const data = await response.json();
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible guardar el usuario.');
        if (!esNuevo) {
            const responseBodegas = await fetch(`/usuarios/${id}/bodegas`, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(idsBodegas)});
            const dataBodegas = await responseBodegas.json();
            if (!responseBodegas.ok) throw new Error(dataBodegas.mensaje || 'No fue posible guardar las asociaciones.');
        }
        await LoadingOverlay.ocultar(); bootstrap.Modal.getOrCreateInstance(document.getElementById('modalUsuario')).hide();
        await mostrarExito(data.mensaje, 'Usuario guardado'); await cargarUsuarios();
    } catch (error) {
        await LoadingOverlay.ocultar();
        mostrarError(error.message, 'No fue posible guardar');
    }
}
