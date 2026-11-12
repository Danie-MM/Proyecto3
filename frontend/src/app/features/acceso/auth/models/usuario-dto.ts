
export interface UsuarioDto {
  id: number;
  idPublico?: string;
  nombreCompleto: string;
  email: string;
  fechaNacimiento: string;
  rol: string;
  activo: boolean;
}
