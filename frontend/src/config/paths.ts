type PathDefinition = {
  path: string;
  getHref: (...args: never[]) => string;
};

type PathsType = {
  [key: string]: PathDefinition | PathsType;
};

export const paths = {
  app: {
    home: {
      path: '/',
      getHref: () => '/',
    },
  },

  auth: {
    register: {
      path: 'register',
      getHref: () => '/auth/register',
    },
    login: {
      path: 'login',
      getHref: () => '/auth/login',
    },
  },
} satisfies PathsType;
