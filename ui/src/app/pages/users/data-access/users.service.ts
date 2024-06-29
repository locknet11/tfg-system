import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from 'src/environments/environment';
import {
  CreateUserRequest,
  UpdateUserRequest,
  User,
  UsersList,
} from './users.model';
import { AccountInfo } from 'src/app/shared/models/global.model';

const baseUrl = environment.baseUrl;

@Injectable()
export class UsersService {
  constructor(private http: HttpClient) {}

  getUsers(page: number, size: number) {
    let params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<UsersList>(`${baseUrl}/user`, { params });
  }

  createUser(userRequest: CreateUserRequest) {
    return this.http.post<AccountInfo>(`${baseUrl}/user`, userRequest);
  }

  deleteUser(id: string) {
    return this.http.delete(`${baseUrl}/user/${id}`);
  }

  getUserById(userId: string) {
    return this.http.get<AccountInfo>(`${baseUrl}/user/${userId}`);
  }

  updateById(request: UpdateUserRequest, userId: string) {
    return this.http.put(`${baseUrl}/user/${userId}`, request);
  }
}
