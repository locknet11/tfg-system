export interface UsersList {
  content: User[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: string;
  moduleAccess: string[];
}

export interface CreateUserRequest extends Omit<User, 'id'> {
  password: string;
}

export interface UpdateUserRequest extends Omit<User, 'id'> {}

export const moduleAccessList = [
  {
    value: 'USERS_MODULE',
    label: $localize`Users`,
  },
];

export const userRoles = [
  {
    value: 'ADMIN',
    label: $localize`Administrator`,
  },
  {
    value: 'USER',
    label: $localize`User`,
  },
];
