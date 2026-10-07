// Variables de estado
let recepcionEstado = null;
let idHojaRecepcion = null;
let idHojaContenedorRecepcion = null;
let procesandoEntradaRecepcion = false;
let modoSinContenedorRecepcion = false;

// Función para obtener un elemento del DOM por su ID
function elementoRecepcion(id) { return document.getElementById(id); }
// Función para actualizar el texto de un elemento del DOM por su ID
function textoRecepcion(id, valor) { const e = elementoRecepcion(id); if (e) e.textContent = valor ?? ''; }
// Función para escapar caracteres especiales en un valor
function escaparRecepcion(valor) {
    const caracteres = {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'};
    return String(valor ?? '').replace(/[&<>"']/g, c => caracteres[c]);
}
// Función para formatear una fecha en formato local
function fechaRecepcion(valor) {
    if (!valor) return 'Sin fecha';
    const fecha = new Date(valor);
    return Number.isNaN(fecha.getTime()) ? escaparRecepcion(valor) : fecha.toLocaleString('es-MX', {day:'2-digit', month:'2-digit', year:'numeric', hour:'2-digit', minute:'2-digit'});
}
// Función para procesar la respuesta de una solicitud fetch
async function respuestaRecepcion(response) {
    if (response.status === 401) { window.location.href = '/login'; throw new Error('La sesión ha expirado.'); }
    const texto = await response.text();
    if (!texto) return {};
    try { return JSON.parse(texto); } catch { return {mensaje: texto}; }
}
// Función para procesar mensajes de negocio
    function mensajeNegocioRecepcion(error) {
    const original = String(error?.message || error || '').trim();
    const mensaje = original.toLowerCase();
    if (mensaje.includes('salida pendiente')) return 'No se puede recibir: el conjunto no tiene una salida pendiente para esta hoja.';
    if (mensaje.includes('no está surtido')) return 'No se puede recibir: el conjunto no está en estado Surtido.';
    if (mensaje.includes('ya fue devuelta')) return 'No se puede recibir: la cantidad surtida de este producto ya fue devuelta.';
    if (mensaje.includes('contenedor') && mensaje.includes('liberada')) return 'No se puede usar: el contenedor ya fue liberado.';
    if (mensaje.includes('carga debe cerrarse')) return 'Primero cierra la carga del contenedor y después libéralo.';
    if (mensaje.includes('no pertenece a la hoja')) return 'El contenedor no pertenece a la hoja seleccionada.';
    if (mensaje.includes('no está disponible para recepción')) return 'La hoja no está disponible para recepción.';
    if (mensaje.includes('conjunto no existe')) return 'El conjunto no existe o el código no es válido.';
    return original || 'No fue posible completar la operación.';
}
// Función para calcular el porcentaje de recepción
function porcentajeRecepcion(valor, total) { return total > 0 ? Math.min(Math.floor((valor * 100) / total), 100) : 0; }
// Función para enfocar un campo de entrada
function enfocarEntradaRecepcion(id) {
    // Usar requestAnimationFrame para asegurar que el elemento esté visible antes de enfocar
    window.requestAnimationFrame(() => {
        // Obtenemos el elemento de entrada y verificamos que no esté deshabilitado ni oculto
        const input = elementoRecepcion(id);
        if (input && !input.disabled && !input.classList.contains('d-none')) {
            input.focus();
            input.select?.();
        }
    });
}
// Función para actualizar el modo de entrada de recepción
function actualizarModoEntradaRecepcion() {
    // Si la recepción está completa, ocultamos todos los bloques de entrada y botones
    if (recepcionEstado?.completo) {
        elementoRecepcion('bloqueSeleccionarContenedorRecepcion')?.classList.add('d-none');
        elementoRecepcion('bloqueEscanearConjuntoRecepcion')?.classList.add('d-none');
        elementoRecepcion('btnCambiarContenedorRecepcion')?.classList.add('d-none');
        elementoRecepcion('btnRecibirSinContenedorRecepcion')?.classList.add('d-none');
        return;
    }
    // Determinamos si hay un contenedor seleccionado o si estamos en modo sin contenedor
    const tieneContenedor = Boolean(idHojaContenedorRecepcion);
    const puedeEscanearConjunto = tieneContenedor || modoSinContenedorRecepcion;
    // Mostramos u ocultamos los bloques de entrada y botones según el estado
    elementoRecepcion('bloqueSeleccionarContenedorRecepcion')?.classList.toggle('d-none', puedeEscanearConjunto);
    elementoRecepcion('bloqueEscanearConjuntoRecepcion')?.classList.toggle('d-none', !puedeEscanearConjunto);
    elementoRecepcion('btnCambiarContenedorRecepcion')?.classList.toggle('d-none', !puedeEscanearConjunto);
    elementoRecepcion('btnRecibirSinContenedorRecepcion')?.classList.toggle('d-none', puedeEscanearConjunto);
    // Habilitamos o deshabilitamos el campo de entrada de conjunto según el estado
    const inputConjunto = elementoRecepcion('codigoConjuntoRecepcion');
    if (inputConjunto) inputConjunto.disabled = !puedeEscanearConjunto || Boolean(recepcionEstado?.completo);
    if (puedeEscanearConjunto && !recepcionEstado?.completo) {
        enfocarEntradaRecepcion('codigoConjuntoRecepcion');
    } else if (!puedeEscanearConjunto) {
        enfocarEntradaRecepcion('codigoContenedorRecepcion');
    }
}
// Función para actualizar la información del contenedor seleccionado en la interfaz
function actualizarContenedorSeleccionadoRecepcion(contenedor) {
    // Actualizamos el ID del contenedor seleccionado y el modo sin contenedor
    idHojaContenedorRecepcion = contenedor?.idHojaContenedor || null;
    if (idHojaContenedorRecepcion) modoSinContenedorRecepcion = false;
    // Actualizamos los textos de la interfaz con la información del contenedor
    textoRecepcion('hhCodigoContenedorRecepcion', contenedor?.codigo || 'Ninguno');
    textoRecepcion('hhDetalleContenedorRecepcion', contenedor ? (contenedor.liberado ? 'Liberado' : 'Activo para retorno') : 'Sin contenedor');
    textoRecepcion('hhEstadoContenedorRecepcion', contenedor ? (contenedor.liberado ? 'Liberado' : 'Seleccionado') : 'Opcional');
    // Actualizamos el modo de entrada de recepción según el estado actual
    actualizarModoEntradaRecepcion();
}
// Función para renderizar los detalles de recepción en la interfaz
function renderizarDetallesRecepcion(detalles) {
    // Obtenemos el contenedor donde se mostrarán los detalles
    const contenedor = elementoRecepcion('hhDetallesRecepcion');
    if (!contenedor) return;
    // Limpiamos el contenido del contenedor antes de agregar los detalles
    contenedor.innerHTML = '';
    // Si no hay detalles, mostramos un mensaje de placeholder
    if (!detalles.length) { contenedor.innerHTML = '<div class="hh-recepcion-placeholder">No hay detalles registrados.</div>'; return; }
    // Iteramos sobre cada detalle y creamos una tarjeta para mostrar su información
    detalles.forEach(detalle => {
        const porcentaje = Math.max(0, Math.min(100, Number(detalle.porcentaje) || 0));
        const tarjeta = document.createElement('div');
        tarjeta.className = 'border rounded p-3';
        tarjeta.innerHTML = `<div class="d-flex justify-content-between gap-2"><strong>${escaparRecepcion(detalle.producto || 'Producto')}</strong><span class="badge ${detalle.completo ? 'text-bg-success' : 'text-bg-secondary'}">${detalle.completo ? 'Completo' : 'Pendiente'}</span></div><div class="small text-muted mt-2">Surtido: ${detalle.cantidadSurtida ?? 0} · Devuelto: ${detalle.cantidadDevuelta ?? 0} · Pendiente: ${detalle.cantidadPendiente ?? 0}</div><div class="progress mt-2" role="progressbar" aria-valuenow="${porcentaje}" aria-valuemin="0" aria-valuemax="100"><div class="progress-bar bg-success" style="width:${porcentaje}%">${porcentaje}%</div></div>`;
        contenedor.appendChild(tarjeta);
    });
}
// Función para renderizar los contenedores pendientes de liberación en la interfaz
function renderizarContenedoresRecepcion(contenedores) {
    // Obtenemos el contenedor donde se mostrarán los contenedores pendientes
    const contenedor = elementoRecepcion('hhContenedoresPendientes');
    if (!contenedor) return;
    // Limpiamos el contenido del contenedor antes de agregar los contenedores pendientes
    contenedor.innerHTML = '';
    // Si no hay contenedores pendientes, mostramos un mensaje de placeholder
    if (!contenedores.length) { contenedor.innerHTML = '<div class="hh-recepcion-placeholder">No hay contenedores pendientes de liberar.</div>'; return; }
    // Iteramos sobre cada contenedor pendiente y creamos una tarjeta para mostrar su información
    contenedores.forEach(item => {
        const tarjeta = document.createElement('div');
        tarjeta.className = 'border rounded p-3';
        const boton = item.fechaCierreCarga && !item.liberado ? `<button type="button" class="btn btn-sm btn-outline-success" data-id-hoja-contenedor="${item.idHojaContenedor}"><i class="bi bi-unlock me-1"></i>Liberar</button>` : `<span class="badge text-bg-warning">Carga abierta</span>`;
        tarjeta.innerHTML = `<div class="d-flex align-items-center justify-content-between gap-2"><div><strong>${escaparRecepcion(item.codigo)}</strong><div class="small text-muted">Asignado: ${fechaRecepcion(item.fechaAsignacion)}</div></div>${boton}</div>`;
        tarjeta.querySelector('button')?.addEventListener('click', () => liberarContenedorRecepcion(item.idHojaContenedor));
        contenedor.appendChild(tarjeta);
    });
}
// Función para renderizar los últimos movimientos de recepción en la interfaz
function renderizarMovimientosRecepcion(movimientos) {
    // Obtenemos el contenedor donde se mostrarán los últimos movimientos
    const contenedor = elementoRecepcion('hhMovimientosRecepcion');
    if (!contenedor) return;
    // Limpiamos el contenido del contenedor antes de agregar los últimos movimientos
    contenedor.innerHTML = '';
    // Si no hay movimientos, mostramos un mensaje de placeholder
    if (!movimientos.length) { contenedor.innerHTML = '<div class="hh-recepcion-placeholder">No hay movimientos recientes.</div>'; return; }
    // Iteramos sobre cada movimiento y creamos un elemento para mostrar su información
    movimientos.forEach(movimiento => {
        const item = document.createElement('div');
        item.className = 'border rounded p-2';
        item.innerHTML = `<div class="d-flex justify-content-between gap-2"><strong>${escaparRecepcion(movimiento.codigoConjunto)}</strong><span class="small text-muted">${fechaRecepcion(movimiento.fecha)}</span></div><div class="small text-muted">${escaparRecepcion(movimiento.producto || 'Producto')} · ${escaparRecepcion(movimiento.codigoContenedor || 'Sin contenedor')} · ${escaparRecepcion(movimiento.usuario || 'Usuario')}</div>`;
        contenedor.appendChild(item);
    });
}
// Función para actualizar el estado de recepción en la interfaz
function actualizarEstadoRecepcion(estado) {
    // Actualizamos la variable global de estado de recepción
    recepcionEstado = estado;
    // Mostramos la sección de operación y ocultamos la sección de selección de hoja
    elementoRecepcion('seccionSeleccionHojaRecepcion')?.classList.add('d-none');
    // Mostramos la sección de operación de recepción
    elementoRecepcion('seccionOperacionRecepcion')?.classList.remove('d-none');
    // Actualizamos los textos de la interfaz con la información del estado de recepción
    textoRecepcion('tituloRecepcionHoja', estado.nombreProyecto || 'Sin proyecto');
    textoRecepcion('hhRecepcionCliente', estado.cliente || 'Sin cliente');
    textoRecepcion('hhFechaRegreso', fechaRecepcion(estado.fechaEstimadaRegreso));
    textoRecepcion('hhRecepcionPorcentaje', estado.porcentaje ?? 0);
    textoRecepcion('hhTotalSurtidoRecepcion', estado.totalSurtido ?? 0);
    textoRecepcion('hhTotalDevueltoRecepcion', estado.totalDevuelto ?? 0);
    textoRecepcion('hhTotalPendienteRecepcion', estado.totalPendiente ?? 0);
    // Actualizamos la barra de progreso de recepción
    const progreso = elementoRecepcion('hhRecepcionProgreso');
    // Calculamos el porcentaje de recepción asegurándonos de que esté entre 0 y 100
    const porcentaje = Math.max(0, Math.min(100, Number(estado.porcentaje) || 0));
    // Actualizamos el ancho de la barra de progreso y su texto
    if (progreso) { progreso.style.width = `${porcentaje}%`; progreso.textContent = `${porcentaje}%`; progreso.setAttribute('aria-valuenow', String(porcentaje)); }
    // Actualizamos la información del contenedor seleccionado, los detalles, los contenedores pendientes y los últimos movimientos
    actualizarContenedorSeleccionadoRecepcion(estado.contenedorActual);
    renderizarDetallesRecepcion(estado.detalles || []);
    renderizarContenedoresRecepcion(estado.contenedoresPendientes || []);
    renderizarMovimientosRecepcion(estado.ultimosMovimientos || []);
}
// Función para cargar el estado de recepción desde la API
async function cargarEstadoRecepcion() {
    // Si no hay un ID de hoja de recepción, no hacemos nada
    if (!idHojaRecepcion) return;
    // Hacemos una solicitud fetch a la API para obtener el estado de recepción
    const response = await fetch(`/hh/recepcion/api/hojas/${idHojaRecepcion}/estado`);
    const data = await respuestaRecepcion(response);
    if (!response.ok) throw new Error(data.mensaje || 'No fue posible cargar el estado de recepción.');
    // Actualizamos la interfaz con el estado de recepción obtenido
    actualizarEstadoRecepcion(data);
}
// Función para seleccionar un contenedor de recepción
async function seleccionarContenedorRecepcion() {
    // Si ya hay un contenedor seleccionado o estamos en modo sin contenedor, no hacemos nada
    const input = elementoRecepcion('codigoContenedorRecepcion');
    // Obtenemos el código del contenedor ingresado por el usuario y lo limpiamos de espacios en blanco
    const codigo = input?.value.trim();
    // Si no se ingresó un código, mostramos un mensaje de error y enfocamos el campo de entrada
    if (!codigo) { await mostrarError('Captura o escanea el código del contenedor.', 'Contenedor requerido'); input?.focus(); return; }
    try {
        // Mostramos un overlay de carga mientras se procesa la selección del contenedor
        LoadingOverlay.mostrar('Seleccionando contenedor...');
        // Hacemos una solicitud fetch a la API para seleccionar el contenedor con el código ingresado
        const response = await fetch(`/hh/recepcion/api/hojas/${idHojaRecepcion}/contenedores`, {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({codigo})});
        // Procesamos la respuesta de la API y obtenemos los datos
        const data = await respuestaRecepcion(response);
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible seleccionar el contenedor.');
        input.value = '';
        // Cargamos nuevamente el estado de recepción para reflejar los cambios en la interfaz
        await cargarEstadoRecepcion();
        // Ocultamos el overlay de carga y mostramos un mensaje de éxito temporal
        await LoadingOverlay.ocultar();
        await mostrarExitoTemporal(data.mensaje || 'Contenedor seleccionado correctamente.', 'Contenedor seleccionado');
    } catch (error) { await LoadingOverlay.ocultar(); await mostrarError(mensajeNegocioRecepcion(error), 'No fue posible seleccionar'); }
}
// Función para registrar la entrada de recepción
async function registrarEntradaRecepcion() {
    // Si ya se está procesando una entrada o la recepción está completa, no hacemos nada
    if (procesandoEntradaRecepcion || recepcionEstado?.completo) return;
    // Obtenemos el código del conjunto ingresado por el usuario y lo limpiamos de espacios en blanco
    const input = elementoRecepcion('codigoConjuntoRecepcion');
    const codigoConjunto = input?.value.trim();
    // Si no se ingresó un código de conjunto, no hacemos nada
    if (!codigoConjunto) return;
    procesandoEntradaRecepcion = true;
    input.disabled = true;
    // Mostramos un overlay de carga mientras se procesa el registro de la entrada
    try {
        // Mostramos un overlay de carga mientras se procesa el registro de la entrada
        LoadingOverlay.mostrar('Registrando entrada...');
        // Hacemos una solicitud fetch a la API para registrar la entrada con el código de conjunto y el ID del contenedor seleccionado
        const response = await fetch(`/hh/recepcion/api/hojas/${idHojaRecepcion}/entradas`, {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({codigoConjunto, idHojaContenedor:idHojaContenedorRecepcion})});
        // Procesamos la respuesta de la API y obtenemos los datos
        const data = await respuestaRecepcion(response);
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible registrar la entrada.');
        input.value = '';
        // Cargamos nuevamente el estado de recepción para reflejar los cambios en la interfaz
        await cargarEstadoRecepcion();
        // Ocultamos el overlay de carga y mostramos un mensaje de éxito temporal
        await LoadingOverlay.ocultar();
        // Mostramos un mensaje de éxito temporal indicando que la entrada se registró correctamente
        await mostrarExitoTemporal(
            data.mensaje || 'Entrada registrada correctamente.',
            data.recepcionCompleta ? 'Recepción completada' : 'Entrada registrada',
            data.recepcionCompleta ? 2200 : 1800);
    } catch (error) { await LoadingOverlay.ocultar(); await mostrarError(mensajeNegocioRecepcion(error), 'No fue posible registrar la entrada'); }
    finally {
        // Restablecemos el estado de procesamiento y habilitamos el campo de entrada
        procesandoEntradaRecepcion = false;
        actualizarModoEntradaRecepcion();
    }
}
// Función para cambiar el contenedor de recepción
function cambiarContenedorRecepcion() {
    idHojaContenedorRecepcion = null;
    modoSinContenedorRecepcion = false;
    actualizarContenedorSeleccionadoRecepcion(null);
}

// Función para continuar sin contenedor de recepción
function continuarSinContenedorRecepcion() {
    idHojaContenedorRecepcion = null;
    modoSinContenedorRecepcion = true;
    actualizarContenedorSeleccionadoRecepcion(null);
}
// Función para liberar un contenedor de recepción
async function liberarContenedorRecepcion(idHojaContenedor) {
    // Mostramos un cuadro de confirmación antes de liberar el contenedor
    const confirmacion = await confirmarAccion({titulo:'¿Liberar contenedor?', mensaje:'La carga debe estar cerrada. Después podrá reutilizarse.', textoConfirmar:'Sí, liberar', colorConfirmar:'#198754'});
    if (!confirmacion.isConfirmed) return;
    try {
        // Mostramos un overlay de carga mientras se procesa la liberación del contenedor
        LoadingOverlay.mostrar('Liberando contenedor...');
        // Hacemos una solicitud fetch a la API para liberar el contenedor con el ID proporcionado
        const response = await fetch(`/hh/recepcion/api/hojas/${idHojaRecepcion}/contenedores/${idHojaContenedor}/liberar`, {method:'POST'});
        const data = await respuestaRecepcion(response);
        // Si la respuesta no es exitosa, lanzamos un error con el mensaje proporcionado por la API
        if (!response.ok) throw new Error(data.mensaje || 'No fue posible liberar el contenedor.');
        // Cargamos nuevamente el estado de recepción para reflejar los cambios en la interfaz
        await cargarEstadoRecepcion();
        // Ocultamos el overlay de carga y mostramos un mensaje de éxito temporal
        await LoadingOverlay.ocultar();
        // Mostramos un mensaje de éxito temporal indicando que el contenedor se liberó correctamente
        await mostrarExitoTemporal(
            data.mensaje || 'Contenedor liberado correctamente.',
            data.recepcionCompleta ? 'Recepción completada' : 'Contenedor liberado');
    } catch (error) { await LoadingOverlay.ocultar(); await mostrarError(mensajeNegocioRecepcion(error), 'No fue posible liberar'); }
}
// Inicializamos los eventos cuando el DOM esté completamente cargado
document.addEventListener('DOMContentLoaded', () => {
    // Obtenemos el elemento principal que contiene el ID de la hoja de recepción
    const principal = document.querySelector('main[data-id-hoja]');
    // Obtenemos el ID de la hoja de recepción desde el atributo data-id-hoja del elemento principal
    idHojaRecepcion = Number(principal?.dataset.idHoja);
    // Validamos que el ID de la hoja de recepción sea un número entero positivo, si no lo es, lo establecemos como null
    if (!Number.isInteger(idHojaRecepcion) || idHojaRecepcion <= 0) idHojaRecepcion = null;
    // Agregamos un evento de envío al formulario de selección de hoja de recepción
    elementoRecepcion('formSeleccionHojaRecepcion')?.addEventListener('submit', event => {
        // Prevenimos el comportamiento predeterminado del formulario para evitar que se envíe
        event.preventDefault();
        // Obtener el ID de la hoja seleccionada desde el campo de entrada y validarlo
        const idHoja = Number(elementoRecepcion('idHojaSeleccionadaRecepcion')?.value);
        // Validamos que el ID de la hoja sea un número entero positivo, si no lo es, mostramos un mensaje de validación y salimos de la función
        if (!Number.isInteger(idHoja) || idHoja <= 0) { event.currentTarget.classList.add('was-validated'); return; }
        // Redirigimos al usuario a la página de recepción de la hoja seleccionada
        window.location.href = `/hh/recepcion/${idHoja}`;
    });
    // Agregamos eventos de clic a los botones de selección, cambio y continuación sin contenedor de recepción
    elementoRecepcion('btnSeleccionarContenedorRecepcion')?.addEventListener('click', seleccionarContenedorRecepcion);
    elementoRecepcion('btnCambiarContenedorRecepcion')?.addEventListener('click', cambiarContenedorRecepcion);
    elementoRecepcion('btnRecibirSinContenedorRecepcion')?.addEventListener('click', continuarSinContenedorRecepcion);
    elementoRecepcion('codigoContenedorRecepcion')?.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); seleccionarContenedorRecepcion(); } });
    elementoRecepcion('codigoConjuntoRecepcion')?.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); registrarEntradaRecepcion(); } });
    // Si hay un ID de hoja de recepción válido, cargamos el estado de recepción desde la API
    if (idHojaRecepcion) LoadingOverlay.ejecutar(cargarEstadoRecepcion, 'Cargando estado de recepción...')
        .catch(async error => await mostrarError(mensajeNegocioRecepcion(error), 'No fue posible cargar la hoja'));
});
