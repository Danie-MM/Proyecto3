
import { UsuarioDto } from './usuario-dto';

export interface RegistroResponse {
  token: string;
  usuario: UsuarioDto;
}
